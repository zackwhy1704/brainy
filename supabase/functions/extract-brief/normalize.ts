import type { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import type { ItemRow, NormalizedContent } from "./types.ts";

const MAX_URL_TEXT_CHARS = 15000;

/** Crude but dependency-free readable-text extraction: strip script/style, then all tags. */
function stripHtml(html: string): string {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function bytesToBase64(bytes: Uint8Array): string {
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}

export async function normalizeContent(
  item: ItemRow,
  storage: SupabaseClient,
): Promise<NormalizedContent> {
  switch (item.source_type) {
    case "text":
      return { text: item.raw_text ?? "" };

    case "url": {
      if (!item.source_uri) return { text: "" };
      const response = await fetch(item.source_uri, {
        headers: { "user-agent": "Mozilla/5.0 (compatible; SecondBrainExtractor/1.0)" },
      });
      const html = await response.text();
      return { text: stripHtml(html).slice(0, MAX_URL_TEXT_CHARS) };
    }

    case "image":
    case "pdf": {
      if (!item.source_uri) return { text: "" };
      const { data, error } = await storage.storage.from("captures").download(item.source_uri);
      if (error || !data) {
        throw new Error(`Storage download failed for ${item.source_uri}: ${error?.message ?? "no data"}`);
      }
      const bytes = new Uint8Array(await data.arrayBuffer());
      const mediaType = item.source_type === "pdf" ? "application/pdf" : (data.type || "image/jpeg");
      return {
        inline: {
          mediaType,
          base64Data: bytesToBase64(bytes),
          kind: item.source_type === "pdf" ? "document" : "image",
        },
      };
    }
  }
}
