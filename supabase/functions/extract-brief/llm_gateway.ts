import type { BriefJson, NormalizedContent } from "./types.ts";

/**
 * Backend-side interface (ARCHITECTURE.md "Where LlmGateway actually sits") — swappable
 * per-provider without touching the orchestration in index.ts. Phase 2 ships exactly one
 * provider (providers/anthropic.ts); this is the seam a second provider would implement.
 */
export interface LlmGateway {
  extract(content: NormalizedContent): Promise<BriefJson>;
}

export class ExtractionError extends Error {}
