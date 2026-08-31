import { GENERAL_PROFILE } from "./general.ts";
import type { ExtractionProfile } from "./types.ts";

export const FACT_CATEGORIES = ["motivation", "compensation", "location", "constraint", "availability", "preference"] as const;

/**
 * Person-scoped facts on top of the general base fields. Every fact carries the verbatim quote
 * that supports it — provenance is the product ("intelligence without provenance is just AI
 * guessing"). The `facts` array is a non-base field, so it lands in briefs.attributes via the
 * normal split; index.ts additionally reconciles it into the versioned `facts` table.
 */
export const RELATIONSHIP_PROFILE: ExtractionProfile = {
  name: "relationship",
  promptPrefix:
    "This content concerns the user's relationship with one or more people (a conversation, call notes, " +
    "a message thread). Extract the general fields, AND for each person named extract person-scoped facts " +
    "about their motivation, compensation, location, constraint, availability, or preference. Use the person's " +
    "name exactly as written. Each fact's `quote` must be a verbatim excerpt from the content that supports it. " +
    "Only record facts the content actually states — never infer beyond it. If a list of KNOWN CURRENT FACTS " +
    "is provided, compare each extracted fact against it: set `matches_fact_id` to the known fact about the same " +
    "person and category. Then decide `changed` by asking: would the OLD statement now be false? Only then is it " +
    "true. Different wording, more or less detail, or added emphasis is NOT a change. Example: known " +
    "'Lives in London' vs new 'Still London-based for now' → changed=false (the old statement remains true). " +
    "Known 'Lives in London' vs new 'Moved to Berlin last month' → changed=true. Content:",
  properties: {
    ...GENERAL_PROFILE.properties,
    facts: {
      type: "array",
      description: "Person-scoped facts stated in the content. Empty if no person is discussed.",
      items: {
        type: "object",
        properties: {
          subject: { type: "string", description: "Person's name exactly as written in the content." },
          category: { type: "string", enum: [...FACT_CATEGORIES] },
          value: { type: "string", description: "The fact, as a short declarative phrase." },
          quote: { type: "string", description: "Verbatim excerpt from the content supporting this fact." },
          confidence: { type: "number", minimum: 0, maximum: 1 },
          matches_fact_id: {
            type: ["string", "null"],
            description: "id of the KNOWN CURRENT FACT about the same person and category, if one was provided; else null.",
          },
          changed: {
            type: "boolean",
            description: "true only if this materially differs from the matched known fact. A reworded restatement is false. false when nothing matched.",
          },
        },
        required: ["subject", "category", "value", "quote", "confidence", "matches_fact_id", "changed"],
      },
    },
  },
  required: [...GENERAL_PROFILE.required, "facts"],
};
