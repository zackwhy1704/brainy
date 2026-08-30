const EMBEDDING_MODEL = "text-embedding-3-small"; // native 1536-dim, matches embeddings.vector's column exactly

export async function embed(text: string, apiKey: string): Promise<number[]> {
  const response = await fetch("https://api.openai.com/v1/embeddings", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({ model: EMBEDDING_MODEL, input: text.slice(0, 8000) }),
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`OpenAI embeddings API ${response.status}: ${body.slice(0, 500)}`);
  }
  const json = await response.json();
  return json.data[0].embedding as number[];
}
