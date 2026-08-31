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
      text: content.text
        ? `${profile.promptPrefix}\n\n${content.text}`
        : `${profile.promptPrefix}\n\n(see attached content)`,
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
        // Extraction is a classification, not creative output, so 0 is the right default. It is
        // NOT a fix for the same/changed verdict on reworded-identical facts: measured with
        // supabase/tests/fact_diff_guard.py, pair (b) was 4/9 correct at 1.0 and 2/5 at 0. The
        // verdict flips on byte-identical wording; the matched fact id is right every time.
        temperature: 0,
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

  /**
   * The focused same/changed call (see LlmGateway.judgeRestatement). Two strings in, one enum out.
   * Kept out of the extraction call on purpose: measured there at 43% correct on paraphrases
   * (fact_diff_guard pair (b)); a single binary question gives the model nothing else to attend to.
   */
  async judgeRestatement(previousValue: string, newValue: string): Promise<boolean> {
    const tool = {
      name: "record_verdict",
      description: "Record whether the new statement restates the known fact or changes it.",
      input_schema: {
        type: "object",
        properties: {
          verdict: {
            type: "string",
            enum: ["restatement", "change"],
            description:
              "restatement: same underlying fact — different wording, more or less detail, or added " +
              "emphasis; the earlier fact is still true. change: the situation itself changed — if the " +
              "new statement is accurate, the earlier fact is no longer true.",
          },
        },
        required: ["verdict"],
      },
    };

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": this.apiKey,
        "anthropic-version": ANTHROPIC_VERSION,
      },
      body: JSON.stringify({
        model: MODEL,
        max_tokens: 128,
        temperature: 0,
        tools: [tool],
        tool_choice: { type: "tool", name: "record_verdict" },
        messages: [{
          role: "user",
          content:
            "A fact about a person was recorded earlier. A new statement about the same person and the " +
            "same aspect of their life was captured later. Decide whether the new statement is a " +
            "restatement of the earlier fact or a genuine change to it. Ask: if the new statement is " +
            "accurate, would the earlier fact now be false? Only then is it a change.\n\n" +
            `EARLIER FACT: ${previousValue}\n` +
            `NEW STATEMENT: ${newValue}`,
        }],
      }),
    });

    if (!response.ok) {
      const body = await response.text();
      throw new ExtractionError(`Anthropic API ${response.status} (judgeRestatement): ${body.slice(0, 500)}`);
    }

    const json = await response.json();
    const toolUse = (json.content as Array<Record<string, unknown>> | undefined)?.find(
      (block) => block.type === "tool_use" && block.name === "record_verdict",
    );
    if (!toolUse) {
      throw new ExtractionError("Anthropic response had no record_verdict tool_use block");
    }
    const verdict = (toolUse.input as { verdict?: string }).verdict;
    if (verdict !== "restatement" && verdict !== "change") {
      throw new ExtractionError(`judgeRestatement returned unexpected verdict: ${verdict}`);
    }
    return verdict === "restatement";
  }
}
