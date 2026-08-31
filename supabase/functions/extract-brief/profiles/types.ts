/**
 * A profile selects a prompt template and an expected output schema — the vertical-pivot lever.
 * Adding a vertical costs one file in this directory, never a schema migration: whatever a
 * profile's schema declares beyond BASE_FIELD_NAMES lands in briefs.attributes (jsonb),
 * everything in BASE_FIELD_NAMES lands in its existing typed column, same as today.
 */
export interface ExtractionProfile {
  name: string;
  /** Prepended to the content in the user message — profile-specific extraction instructions. */
  promptPrefix: string;
  /** Full JSON-schema `properties` object for the tool-forced extraction call. */
  properties: Record<string, unknown>;
  required: string[];
}

/** Every profile must produce these — they map to briefs' existing typed columns. Anything else
 * a profile's `properties` adds is an attribute, stored in briefs.attributes without a migration. */
export const BASE_FIELD_NAMES = [
  "summary",
  "entities",
  "people",
  "topics",
  "tasks",
  "decisions",
  "importance",
] as const;
