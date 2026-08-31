import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { AnthropicGateway } from "./providers/anthropic.ts";
import { normalizeContent } from "./normalize.ts";
import { embed } from "./embeddings.ts";
import { ExtractionError } from "./llm_gateway.ts";
import { resolveProfile } from "./profiles/registry.ts";
import { BASE_FIELD_NAMES } from "./profiles/types.ts";
import type { ItemRow } from "./types.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const ANTHROPIC_API_KEY = Deno.env.get("ANTHROPIC_API_KEY")!;
const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY")!;

/** Payload isn't re-verified here — Supabase's edge runtime already validated the signature
 * (verify_jwt=true); this only reads claims already known to be authentic. */
function decodeJwtPayload(authHeader: string | null): { sub?: string; role?: string } {
  const token = authHeader?.replace(/^Bearer\s+/i, "");
  if (!token) return {};
  const segment = token.split(".")[1];
  if (!segment) return {};
  const padded = segment.replace(/-/g, "+").replace(/_/g, "/").padEnd(segment.length + ((4 - (segment.length % 4)) % 4), "=");
  try {
    return JSON.parse(atob(padded));
  } catch {
    return {};
  }
}

/** Splits the profile's raw extraction result into briefs' fixed typed columns (BASE_FIELD_NAMES)
 * plus everything else (attributes jsonb) — this is the whole "no migration for a new profile"
 * seam: a profile's extra schema properties just fall into the second bucket automatically. */
function splitResult(raw: Record<string, unknown>): { base: Record<string, unknown>; attributes: Record<string, unknown> } {
  const base: Record<string, unknown> = {};
  const attributes: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(raw)) {
    if ((BASE_FIELD_NAMES as readonly string[]).includes(key)) base[key] = value;
    else attributes[key] = value;
  }
  return { base, attributes };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "POST only" }), { status: 405 });
  }

  let itemId: string;
  try {
    const body = await req.json();
    itemId = body.item_id;
    if (!itemId) throw new Error("missing item_id");
  } catch {
    return new Response(JSON.stringify({ error: "expected JSON body { item_id }" }), { status: 400 });
  }

  const claims = decodeJwtPayload(req.headers.get("authorization"));
  const service = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

  const { data: item, error: itemError } = await service
    .from("items")
    .select("id, user_id, source_type, source_uri, raw_text, title, profile")
    .eq("id", itemId)
    .maybeSingle<ItemRow>();

  if (itemError || !item) {
    return new Response(JSON.stringify({ error: `item ${itemId} not found: ${itemError?.message}` }), { status: 404 });
  }

  // service_role calls (the DB trigger) skip this; an end-user retry call must own the item.
  if (claims.role !== "service_role" && claims.sub !== item.user_id) {
    return new Response(JSON.stringify({ error: "not authorized for this item" }), { status: 403 });
  }

  const nowIso = new Date().toISOString();

  try {
    const content = await normalizeContent(item, service);
    const profile = resolveProfile(item.profile);
    const gateway = new AnthropicGateway(ANTHROPIC_API_KEY);
    const result = await gateway.extract(content, profile);
    const { base, attributes } = splitResult(result);

    const summary = typeof base.summary === "string" ? base.summary : "";
    const entities = Array.isArray(base.entities) ? base.entities as string[] : [];
    const people = Array.isArray(base.people) ? base.people as string[] : [];
    const topics = Array.isArray(base.topics) ? base.topics as string[] : [];
    const tasks = Array.isArray(base.tasks) ? base.tasks as string[] : [];
    const decisions = Array.isArray(base.decisions) ? base.decisions as string[] : [];
    const importance = typeof base.importance === "number" ? base.importance : null;

    const embeddingInput = [summary, ...topics, ...entities].filter(Boolean).join("\n") ||
      content.text || item.title || "captured content";
    const vector = await embed(embeddingInput, OPENAI_API_KEY);

    const { error: upsertError } = await service.from("briefs").upsert(
      {
        item_id: item.id,
        status: "ready",
        summary,
        entities,
        topics,
        tasks,
        importance,
        attributes,
        failure_reason: null,
        updated_at: nowIso,
      },
      { onConflict: "item_id" },
    );
    if (upsertError) throw new ExtractionError(`briefs upsert failed: ${upsertError.message}`);

    // Idempotent on retry: people/decisions are re-derived, not appended to.
    await service.from("people").delete().eq("item_id", item.id);
    if (people.length > 0) {
      await service.from("people").insert(people.map((name) => ({ user_id: item.user_id, item_id: item.id, name })));
    }

    await service.from("decisions").delete().eq("item_id", item.id);
    if (decisions.length > 0) {
      await service.from("decisions").insert(
        decisions.map((description) => ({ user_id: item.user_id, item_id: item.id, description })),
      );
    }

    await service.from("embeddings").delete().eq("item_id", item.id).eq("chunk_index", 0);
    await service.from("embeddings").insert({
      item_id: item.id,
      chunk_index: 0,
      vector,
      model: "text-embedding-3-small",
    });

    return new Response(JSON.stringify({ status: "ready", profile: profile.name, attributes }), { status: 200 });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await service.from("briefs").upsert(
      {
        item_id: item.id,
        status: "failed",
        failure_reason: message.slice(0, 1000),
        updated_at: nowIso,
      },
      { onConflict: "item_id" },
    );
    return new Response(JSON.stringify({ status: "failed", error: message }), { status: 200 });
  }
});
