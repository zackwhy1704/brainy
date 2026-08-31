import type { NormalizedContent } from "./types.ts";
import type { ExtractionProfile } from "./profiles/types.ts";

/**
 * Backend-side interface (ARCHITECTURE.md "Where LlmGateway actually sits") — swappable
 * per-provider without touching the orchestration in index.ts. Phase 2 ships exactly one
 * provider (providers/anthropic.ts). Returns the raw parsed tool-call object — the shape varies
 * per profile now, so index.ts is what splits it into typed columns vs. attributes.
 */
export interface LlmGateway {
  extract(content: NormalizedContent, profile: ExtractionProfile): Promise<Record<string, unknown>>;

  /**
   * Focused binary judgment for fact reconciliation: given a previously recorded fact value and a
   * newly extracted value for the same person + category, is the new one a restatement of the same
   * underlying fact, or a genuine change? One question, two strings — deliberately NOT part of the
   * extraction call: the same verdict buried in the big extraction schema was measured at 43%
   * correct on paraphrases (fact_diff_guard pair (b), 6/14 across temperatures).
   */
  judgeRestatement(previousValue: string, newValue: string): Promise<boolean>;
}

export class ExtractionError extends Error {}
