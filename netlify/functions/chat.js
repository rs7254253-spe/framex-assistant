// Deploy this on Netlify. Set GEMINI_API_KEY as an environment variable
// in your Netlify site settings (Site configuration -> Environment variables).
// Get a FREE key at https://aistudio.google.com/app/apikey — no credit card needed.

const SYSTEM_PROMPT = `You are Rex, Rohit's personal spoken AI assistant. Rohit is a freelance video editor, colorist, and graphic designer from Rajasthan, India (runs Framex Studio), who is actively improving his spoken English. If he asks your name, say you're Rex.

Every time he speaks to you, do this in order:
1. Look at his exact words (a raw speech transcript). If there is a grammar, word-choice, or phrasing mistake, give a short correction on its own line starting with "Correction: " followed by the fixed sentence. If his sentence was already fine, skip this line entirely.
2. Then respond naturally to what he actually said or asked, drawing on strong knowledge of color grading, DaVinci Resolve, video editing, graphic design, Canva, fitness and workouts, YouTube growth, freelancing, and general world/tech knowledge.

Keep the answer part conversational and fairly short (2-4 sentences) since it will be read aloud by text-to-speech. Never use markdown, asterisks, bullet points, or headers — plain spoken sentences only.`;

const GEMINI_MODEL = "gemini-2.5-flash";

exports.handler = async function (event) {
  if (event.httpMethod !== "POST") {
    return { statusCode: 405, body: "Method Not Allowed" };
  }

  try {
    const { messages } = JSON.parse(event.body);

    // Convert {role: "user"/"assistant", content} into Gemini's {role: "user"/"model", parts}
    const contents = messages.map((m) => ({
      role: m.role === "assistant" ? "model" : "user",
      parts: [{ text: m.content }]
    }));

    const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${process.env.GEMINI_API_KEY}`;

    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
        contents: contents,
        generationConfig: { maxOutputTokens: 300 }
      })
    });

    const data = await response.json();
    const reply =
      data?.candidates?.[0]?.content?.parts?.map((p) => p.text).join("\n").trim() || "";

    return {
      statusCode: 200,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reply })
    };
  } catch (err) {
    return {
      statusCode: 500,
      body: JSON.stringify({ error: err.message })
    };
  }
};
