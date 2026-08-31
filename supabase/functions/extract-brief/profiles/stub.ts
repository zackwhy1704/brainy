import { GENERAL_PROFILE } from "./general.ts";
import type { ExtractionProfile } from "./types.ts";

/**
 * Proof-of-seam only — deliberately not a real vertical. Adds exactly one extra field
 * ("stub_note") beyond GENERAL_PROFILE's base fields, to prove it lands in briefs.attributes
 * with no migration. A real vertical profile is a new file shaped like this one, not a schema
 * change — that's the whole point of this turn's refactor.
 */
export const STUB_PROFILE: ExtractionProfile = {
  name: "stub",
  promptPrefix: "Extract structured information from the following captured content. This is a " +
    "proof-of-concept profile — also include one extra field, stub_note, a short made-up string " +
    "unrelated to the content (e.g. a random word), purely to prove attribute-only fields round-trip:",
  properties: {
    ...GENERAL_PROFILE.properties,
    stub_note: { type: "string", description: "Any short string — proves this profile's extra field round-trips." },
  },
  required: [...GENERAL_PROFILE.required, "stub_note"],
};
