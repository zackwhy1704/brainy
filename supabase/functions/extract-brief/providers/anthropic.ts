import type { LlmGateway } from "../llm_gateway.ts";
import { ExtractionError } from "../llm_gateway.ts";
import type { BriefJson, NormalizedContent } from "../types.ts";

const MODEL = "claude-haiku-4-5-20251001";
const ANTHROPIC_VERSION = "2023-06-01";

// Tool-forcing, not prose-JSON parsing: Claude has no dedicated "JSON mode", but a forced
// tool call's `input` is already a parsed object per the Messages API, which is the reliable
// way to get a shape that matches the extraction schema instead of parsing free text.
const RECORD_BRIEF_TOOL = {
  name: "record_brief",
  description: "Record the structured extraction of the captured content.",
  input_schema: {
    type: "object",
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
  },
};

export class AnthropicGateway implements LlmGateway {
  constructor(private readonly apiKey: string) {}

  async extract(content: NormalizedContent): Promise<BriefJson> {
    const userContent: Record<string, unknown>[] = [];
    if (content.inline) {
      userContent.push({
        type: content.inline.kind,
        source: { type: "base64", media_type: content.inline.mediaType, data: content.inline.base64Data },
      });
    }
    userContent.push({
      type: "text",
      text: content.text
        ? `Extract structured information from the following captured content:\n\n${content.text}`
        : "Extract structured information from the attached content.",
    });

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": this.apiKey,
        "anthropic-version": ANTHROPIC_VERSION,
      },
      body: JSON.stringify({
        model: MODEL,
        max_tokens: 1024,
        tools: [RECORD_BRIEF_TOOL],
        tool_choice: { type: "tool", name: "record_brief" },
        messages: [{ role: "user", content: userContent }],
      }),
    });

    if (!response.ok) {
      const body = await response.text();
      throw new ExtractionError(`Anthropic API ${response.status}: ${body.slice(0, 500)}`);
    }

    const json = await response.json();
    const toolUse = (json.content as Array<Record<string, unknown>> | undefined)?.find(
      (block) => block.type === "tool_use" && block.name === "record_brief",
    );
    if (!toolUse) {
      throw new ExtractionError("Anthropic response had no record_brief tool_use block");
    }

    return toolUse.input as BriefJson;
  }
}
