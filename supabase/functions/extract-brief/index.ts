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

interface ExtractedFact {
  subject: string;
  category: string;
  value: string;
  quote: string;
  confidence?: number;
}

const normalize = (s: string) => s.trim().toLowerCase().replace(/\s+/g, " ");
const escapeLike = (s: string) => s.replace(/[%_\\]/g, (c) => `\\${c}`);

/**
 * Versioned facts with provenance. For each extracted fact, look up the current (non-superseded)
 * fact for the same subject+category:
 *   - none        → insert
 *   - same value  → corroborate: link this item as an additional source, no new fact row
 *   - different   → insert the new fact and mark the old one superseded_by it (a recorded change)
 * Facts are never UPDATEd in place (DB trigger enforces it). Subject matching is case-insensitive
 * string equality only — no entity resolution; two spellings of one person diverge, by design for now.
 * Re-running extraction for the same item is a no-op for facts it already produced.
 */
async function reconcileFacts(
  // deno-lint-ignore no-explicit-any
  service: any,
  item: ItemRow,
  raw: unknown,
): Promise<{ inserted: number; corroborated: number; superseded: number; skipped: number }> {
  const counts = { inserted: 0, corroborated: 0, superseded: 0, skipped: 0 };
  if (!Array.isArray(raw)) return counts;

  for (const candidate of raw as ExtractedFact[]) {
    if (!candidate?.subject?.trim() || !candidate.category || !candidate.value?.trim() || !candidate.quote?.trim()) {
      counts.skipped++;
      continue;
    }
    const subject = candidate.subject.trim();
    const value = candidate.value.trim();

    const { data: current, error } = await service
      .from("facts")
      .select("id, value, source_item_id")
      .eq("user_id", item.user_id)
      .ilike("subject", escapeLike(subject))
      .eq("category", candidate.category)
      .is("superseded_by", null)
      .order("valid_from", { ascending: false })
      .limit(1)
      .maybeSingle();
    if (error) throw new ExtractionError(`facts lookup failed: ${error.message}`);

    if (current?.source_item_id === item.id) {
      counts.skipped++; // already reconciled from this item (retry)
      continue;
    }

    if (current && normalize(current.value) === normalize(value)) {
      const { error: linkError } = await service
        .from("fact_sources")
        .upsert(
          { user_id: item.user_id, fact_id: current.id, item_id: item.id, quote: candidate.quote.trim() },
          { onConflict: "fact_id,item_id", ignoreDuplicates: true },
        );
      if (linkError) throw new ExtractionError(`fact_sources upsert failed: ${linkError.message}`);
      counts.corroborated++;
      continue;
    }

    const { data: inserted, error: insertError } = await service
      .from("facts")
      .insert({
        user_id: item.user_id,
        subject,
        category: candidate.category,
        value,
        quote: candidate.quote.trim(),
        confidence: typeof candidate.confidence === "number" ? candidate.confidence : null,
        valid_from: item.captured_at,
        source_item_id: item.id,
      })
      .select("id")
      .single();
    if (insertError) throw new ExtractionError(`facts insert failed: ${insertError.message}`);

    if (current) {
      const { error: supersedeError } = await service
        .from("facts")
        .update({ superseded_by: inserted.id })
        .eq("id", current.id);
      if (supersedeError) throw new ExtractionError(`facts supersede failed: ${supersedeError.message}`);
      counts.superseded++;
    } else {
      counts.inserted++;
    }
  }
  return counts;
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
    .select("id, user_id, source_type, source_uri, raw_text, title, profile, captured_at")
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

    // Versioned facts — only the relationship profile emits a `facts` array; others fall through
    // with zero counts. Runs after the brief/embedding writes and is deliberately non-fatal: a
    // facts failure must not flip an already-written ready brief to failed. Logged, not swallowed.
    let factCounts: Record<string, unknown>;
    try {
      factCounts = await reconcileFacts(service, item, attributes.facts);
    } catch (factError) {
      const msg = factError instanceof Error ? factError.message : String(factError);
      console.error(`reconcileFacts failed for item ${item.id}: ${msg}`);
      factCounts = { error: msg };
    }

    return new Response(JSON.stringify({ status: "ready", profile: profile.name, attributes, facts: factCounts }), { status: 200 });
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
