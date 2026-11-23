export async function onRequestGet({ env }) {
  return Response.json({
    configured: Boolean(env.GEMINI_API_KEY && env.GEMINI_API_KEY.trim()),
  });
}

export async function onRequestPost({ request, env }) {
  const apiKey = (env.GEMINI_API_KEY || '').trim();
  if (!apiKey) {
    return Response.json(
      {
        error: 'GEMINI_API_KEY belum diatur di Cloudflare Pages Environment Variables.',
        needsClientKey: true,
      },
      { status: 501 },
    );
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: 'Payload JSON tidak valid.' }, { status: 400 });
  }

  const { mediaBase64, mimeType, durationMs } = body || {};
  if (!mediaBase64 || !mimeType) {
    return Response.json({ error: 'Data audio/video tidak lengkap.' }, { status: 400 });
  }

  const safeDuration = Math.max(1000, Number(durationMs) || 15000);
  const prompt = [
    'Transkripsikan seluruh ucapan, dialog, narasi, atau lirik lagu dari audio video ini dengan akurat dalam bahasa aslinya (contoh: Indonesia, Jawa, Sunda, Arab, Inggris sesuai suara).',
    'Pecah menjadi potongan subtitle pendek yang enak dibaca (maksimal 2-6 kata per segmen).',
    `Setiap segmen wajib memiliki startMs dan endMs yang tepat antara 0 sampai ${safeDuration} milidetik dengan endMs > startMs.`,
    'Jika audio hanya musik instrumental tanpa vokal, kembalikan segments array kosong [].',
  ].join('\n');

  const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(apiKey)}`;

  const payload = {
    contents: [
      {
        parts: [
          {
            inline_data: {
              mime_type: mimeType,
              data: mediaBase64,
            },
          },
          { text: prompt },
        ],
      },
    ],
    generationConfig: {
      responseMimeType: 'application/json',
      responseSchema: {
        type: 'OBJECT',
        properties: {
          segments: {
            type: 'ARRAY',
            items: {
              type: 'OBJECT',
              properties: {
                startMs: { type: 'INTEGER' },
                endMs: { type: 'INTEGER' },
                text: { type: 'STRING' },
              },
              required: ['startMs', 'endMs', 'text'],
            },
          },
        },
        required: ['segments'],
      },
    },
  };

  try {
    const res = await fetch(geminiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    if (!res.ok) {
      const msg = data?.error?.message || `Gemini HTTP ${res.status}`;
      return Response.json({ error: msg }, { status: res.status });
    }
    const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || '{"segments":[]}';
    const parsed = JSON.parse(rawText);
    return Response.json({ segments: Array.isArray(parsed.segments) ? parsed.segments : [] });
  } catch (err) {
    return Response.json(
      { error: err?.message || 'Gagal menghubungi Gemini API dari Cloudflare Worker.' },
      { status: 500 },
    );
  }
}
