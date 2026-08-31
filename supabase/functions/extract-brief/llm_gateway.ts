import type { NormalizedContent } from "./types.ts";
import type { ExtractionProfile } from "./profiles/types.ts";

/**
 * Backend-side interface (ARCHITECTURE.md "Where LlmGateway actually sits") — swappable
 * per-provider without touching the orchestration in index.ts. Phase 2 ships exactly one
 * provider (providers/anthropic.ts). Returns the raw parsed tool-call object — the shape varies
 * per profile now, so index.ts is what splits it into typed columns vs. attributes.
 *
 * `context` is optional per-call text placed between the profile's prompt prefix and the content
 * (e.g. the subject's known current facts, so the model can judge "same fact reworded" vs. "changed").
 */
export interface LlmGateway {
  extract(content: NormalizedContent, profile: ExtractionProfile, context?: string): Promise<Record<string, unknown>>;
}

export class ExtractionError extends Error {}
