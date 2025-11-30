package com.example.mobilecw.sync;

import android.text.TextUtils;
import android.util.Log;

import com.example.mobilecw.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight HTTP client that talks to the Gemini embeddings API.
 *
 * The call runs on a background thread (invoked by {@link VectorSyncManager}). The class
 * is intentionally synchronous to keep the implementation simple. Callers must never invoke
 * this from the main thread.
 */
public class GeminiEmbeddingService {

    private static final String TAG = "GeminiEmbeddingService";
    private static final String MODEL_NAME = "models/gemini-2.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/" + MODEL_NAME + ":embedContent";
    private static final int MAX_PROMPT_LENGTH = 2000;

    private final String apiKey;

    public GeminiEmbeddingService() {
        this.apiKey = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(apiKey)) {
            Log.w(TAG, "Gemini API key is empty in BuildConfig");
        } else {
            // Log first 10 chars for debugging (never log full key)
            String preview = apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey;
            Log.d(TAG, "Gemini API key loaded (preview: " + preview + ", length: " + apiKey.length() + ")");
        }
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(apiKey);
    }

    /**
     * Requests an embedding for the provided chunk of text.
     *
     * @param firebaseUid active Firebase user (for context in the prompt)
     * @param chunkType   high-level category (hike_description, observation_note, etc.)
     * @param chunkId     unique chunk identifier (used only for prompt context)
     * @param text        text to embed
     * @return float array representing the embedding or null if anything failed
     */
    public float[] fetchEmbedding(String firebaseUid, String chunkType, String chunkId, String text) {
        if (!isConfigured()) {
            Log.w(TAG, "Gemini API key missing; cannot fetch embeddings");
            return null;
        }
        if (TextUtils.isEmpty(text)) {
            return null;
        }

        String prompt = buildPrompt(firebaseUid, chunkType, chunkId, text);
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT + "?key=" + apiKey);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            byte[] payloadBytes = buildPayload(prompt).getBytes(StandardCharsets.UTF_8);
            OutputStream os = connection.getOutputStream();
            os.write(payloadBytes);
            os.flush();

            int statusCode = connection.getResponseCode();
            InputStream inputStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readFully(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                Log.e(TAG, "Gemini API error (" + statusCode + "): " + responseBody);
                return null;
            }
            return parseEmbedding(responseBody);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to fetch embedding", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildPrompt(String firebaseUid, String chunkType, String chunkId, String text) {
        String trimmed = text.length() > MAX_PROMPT_LENGTH
                ? text.substring(0, MAX_PROMPT_LENGTH)
                : text;
        return "You are Gemini 2.5 Flash acting strictly as an embedder. "
                + "Return only the raw embedding without commentary.\n"
                + "User UID: " + firebaseUid + "\n"
                + "Chunk ID: " + chunkId + "\n"
                + "Chunk Type: " + chunkType + "\n"
                + "Text:\n"
                + trimmed;
    }

    private String buildPayload(String promptText) throws JSONException {
        // Gemini Embedding API format: { "model": "...", "content": { "parts": [{ "text": "..." }] } }
        JSONObject textPart = new JSONObject().put("text", promptText);
        JSONArray parts = new JSONArray().put(textPart);
        JSONObject content = new JSONObject().put("parts", parts);

        JSONObject body = new JSONObject();
        body.put("model", MODEL_NAME);
        body.put("content", content);  // Note: "content" (singular), not "contents"
        return body.toString();
    }

    private float[] parseEmbedding(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        if (!json.has("embedding")) {
            Log.e(TAG, "Gemini response missing embedding: " + responseBody);
            return null;
        }
        JSONObject embeddingObject = json.getJSONObject("embedding");
        JSONArray values = embeddingObject.optJSONArray("values");
        if (values == null) {
            Log.e(TAG, "Gemini response missing values array");
            return null;
        }
        float[] embedding = new float[values.length()];
        for (int i = 0; i < values.length(); i++) {
            embedding[i] = (float) values.getDouble(i);
        }
        return embedding;
    }

    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}


