import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY")!;
const ANTHROPIC_API_KEY = Deno.env.get("ANTHROPIC_API_KEY")!;

const EMBEDDING_MODEL = "text-embedding-3-small";
const CHAT_MODEL = "claude-haiku-4-5-20251001";
const ANTHROPIC_VERSION = "2023-06-01";
const MATCH_COUNT = 5;
// Cosine similarity (1 - vector distance) below this is treated as "not actually relevant" even
// though pgvector top-k always returns *something*. Tuned against the 12 gated items with
// debug=true: on-topic hits scored 0.62–0.65, unrelated items 0.09–0.24. 0.2 let a 0.236 noise
// item into context; 0.5 leaves little headroom for paraphrased questions. 0.3 sits in the gap.
const SIMILARITY_THRESHOLD = 0.3;

interface MatchRow {
  item_id: string;
  similarity: number;
  summary: string | null;
  title: string | null;
  source_type: string;
  raw_text: string | null;
  source_uri: string | null;
}

/** Same fallback chain the Android Home card uses: nothing captured so far has a `title`
 * (Phase 2 never fills it), so text items were all surfacing as "Untitled" citations. */
function citationLabel(m: MatchRow): string {
  if (m.title) return m.title;
  if (m.source_type === "url" && m.source_uri) return m.source_uri;
  const text = m.raw_text?.trim();
  if (text) return text.length > 60 ? `${text.slice(0, 60)}…` : text;
  if (m.source_type === "image") return "Screenshot";
  if (m.source_type === "pdf") return "PDF";
  return m.summary?.slice(0, 60) ?? "Untitled";
}

async function embed(text: string): Promise<number[]> {
  const response = await fetch("https://api.openai.com/v1/embeddings", {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${OPENAI_API_KEY}` },
    body: JSON.stringify({ model: EMBEDDING_MODEL, input: text.slice(0, 8000) }),
  });
  if (!response.ok) throw new Error(`OpenAI embeddings API ${response.status}: ${(await response.text()).slice(0, 500)}`);
  const json = await response.json();
  return json.data[0].embedding as number[];
}

/** Constrains citations structurally, not just by prompt instruction: the schema's enum is built
 * from the actual retrieved ids, so the model cannot cite an item it wasn't given. */
async function answerConstrainedTo(question: string, matches: MatchRow[]): Promise<{ answer: string; citedItemIds: string[] }> {
  const context = matches
    .map((m, i) => {
      const body = m.summary ?? m.raw_text ?? m.source_uri ?? "(no content)";
      return `[${i + 1}] item_id=${m.item_id} title=${m.title ?? "untitled"}\n${body}`;
    })
    .join("\n\n");

  const tool = {
    name: "record_answer",
    description: "Record the answer, constrained to the provided items only.",
    input_schema: {
      type: "object",
      properties: {
        answer: { type: "string", description: "Answer the question using ONLY the provided items. If they don't actually answer it, say so plainly." },
        cited_item_ids: {
          type: "array",
          items: { type: "string", enum: matches.map((m) => m.item_id) },
          description: "item_id values (from the list above) the answer actually draws on. Never invent one.",
        },
      },
      required: ["answer", "cited_item_ids"],
    },
  };

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": ANTHROPIC_API_KEY,
      "anthropic-version": ANTHROPIC_VERSION,
    },
    body: JSON.stringify({
      model: CHAT_MODEL,
      max_tokens: 1024,
      tools: [tool],
      tool_choice: { type: "tool", name: "record_answer" },
      messages: [{
        role: "user",
        content: `Question: ${question}\n\nRetrieved items:\n\n${context}`,
      }],
    }),
  });

  if (!response.ok) throw new Error(`Anthropic API ${response.status}: ${(await response.text()).slice(0, 500)}`);
  const json = await response.json();
  const toolUse = (json.content as Array<Record<string, unknown>> | undefined)?.find(
    (block) => block.type === "tool_use" && block.name === "record_answer",
  );
  if (!toolUse) throw new Error("Anthropic response had no record_answer tool_use block");
  const input = toolUse.input as { answer: string; cited_item_ids: string[] };
  return { answer: input.answer, citedItemIds: input.cited_item_ids };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "POST only" }), { status: 405 });
  }

  let question: string;
  let debug = false;
  try {
    const body = await req.json();
    question = body.question;
    debug = body.debug === true;
    if (!question || typeof question !== "string") throw new Error("missing question");
  } catch {
    return new Response(JSON.stringify({ error: "expected JSON body { question }" }), { status: 400 });
  }

  // Scoped with the caller's own JWT (not service_role) — RLS on items/embeddings/briefs
  // restricts match_items() to this user's own rows, same as every other authenticated call.
  const authHeader = req.headers.get("authorization") ?? "";
  const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    global: { headers: { Authorization: authHeader } },
  });

  try {
    const queryEmbedding = await embed(question);
    const { data, error } = await userClient.rpc("match_items", {
      query_embedding: queryEmbedding,
      match_count: MATCH_COUNT,
    });
    if (error) throw new Error(`match_items RPC failed: ${error.message}`);

    const allMatches = (data as MatchRow[] | null) ?? [];
    // debug=true: return every top-k candidate with its raw similarity, before thresholding —
    // used to tune SIMILARITY_THRESHOLD against real captured data rather than guessing.
    const debugScores = debug
      ? allMatches.map((m) => ({ itemId: m.item_id, similarity: Number(m.similarity.toFixed(3)), label: citationLabel(m) }))
      : undefined;
    const matches = allMatches.filter((m) => m.similarity >= SIMILARITY_THRESHOLD);

    if (matches.length === 0) {
      // Never answer from model knowledge without a citation — this is the explicit
      // no-relevant-retrieval state, not a fallback to general knowledge.
      return new Response(
        JSON.stringify({ hasResults: false, answer: null, citations: [], debugScores }),
        { status: 200 },
      );
    }

    const { answer, citedItemIds } = await answerConstrainedTo(question, matches);
    const citations = matches
      .filter((m) => citedItemIds.includes(m.item_id))
      .map((m) => ({ itemId: m.item_id, title: citationLabel(m) }));

    return new Response(JSON.stringify({ hasResults: true, answer, citations, debugScores }), { status: 200 });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return new Response(JSON.stringify({ error: message }), { status: 500 });
  }
});
