import type { ExtractionProfile } from "./types.ts";

/** Default profile — the schema that shipped before profiles existed, unchanged. */
export const GENERAL_PROFILE: ExtractionProfile = {
  name: "general",
  promptPrefix: "Extract structured information from the following captured content:",
  properties: {
    summary: { type: "string", description: "1-3 sentence summary of the content." },
    entities: { type: "array", items: { type: "string" }, description: "Notable named entities (products, orgs, places, concepts)." },
    people: { type: "array", items: { type: "string" }, description: "Person names mentioned, as written." },
    topics: { type: "array", items: { type: "string" }, description: "Short topic tags, lowercase." },
    tasks: { type: "array", items: { type: "string" }, description: "Actionable to-dos implied or stated." },
    decisions: { type: "array", items: { type: "string" }, description: "Decisions stated or implied." },
    importance: { type: "integer", minimum: 1, maximum: 5, description: "1 = trivial, 5 = critical to remember." },
  },
  required: ["summary", "entities", "people", "topics", "tasks", "decisions", "importance"],
};
