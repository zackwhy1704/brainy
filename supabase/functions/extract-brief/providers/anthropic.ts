import type { LlmGateway } from "../llm_gateway.ts";
import { ExtractionError } from "../llm_gateway.ts";
import type { NormalizedContent } from "../types.ts";
import type { ExtractionProfile } from "../profiles/types.ts";

const MODEL = "claude-haiku-4-5-20251001";
const ANTHROPIC_VERSION = "2023-06-01";
const TOOL_NAME = "record_extraction";

export class AnthropicGateway implements LlmGateway {
  constructor(private readonly apiKey: string) {}

  async extract(content: NormalizedContent, profile: ExtractionProfile): Promise<Record<string, unknown>> {
    const tool = {
      name: TOOL_NAME,
      description: "Record the structured extraction of the captured content.",
      // Tool-forcing, not prose-JSON parsing: Claude has no dedicated "JSON mode", but a forced
      // tool call's `input` is already a parsed object per the Messages API — reliable, and the
      // schema itself is per-profile now, so a new vertical is a new properties object, not new
      // parsing code.
      input_schema: {
        type: "object",
        properties: profile.properties,
        required: profile.required,
      },
    };

    const userContent: Record<string, unknown>[] = [];
    if (content.inline) {
      userContent.push({
        type: content.inline.kind,
        source: { type: "base64", media_type: content.inline.mediaType, data: content.inline.base64Data },
      });
    }
    userContent.push({
      type: "text",
      text: content.text ? `${profile.promptPrefix}\n\n${content.text}` : `${profile.promptPrefix}\n\n(see attached content)`,
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
        tools: [tool],
        tool_choice: { type: "tool", name: TOOL_NAME },
        messages: [{ role: "user", content: userContent }],
      }),
    });

    if (!response.ok) {
      const body = await response.text();
      throw new ExtractionError(`Anthropic API ${response.status}: ${body.slice(0, 500)}`);
    }

    const json = await response.json();
    const toolUse = (json.content as Array<Record<string, unknown>> | undefined)?.find(
      (block) => block.type === "tool_use" && block.name === TOOL_NAME,
    );
    if (!toolUse) {
      throw new ExtractionError(`Anthropic response had no ${TOOL_NAME} tool_use block`);
    }

    return toolUse.input as Record<string, unknown>;
  }
}
