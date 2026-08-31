export interface NormalizedContent {
  // Either plain text, or an inline document/image for a vision-capable model.
  text?: string;
  inline?: { mediaType: string; base64Data: string; kind: "image" | "document" };
}

export interface ItemRow {
  id: string;
  user_id: string;
  source_type: "url" | "text" | "image" | "pdf";
  source_uri: string | null;
  raw_text: string | null;
  title: string | null;
  profile: string;
  captured_at: string;
}
