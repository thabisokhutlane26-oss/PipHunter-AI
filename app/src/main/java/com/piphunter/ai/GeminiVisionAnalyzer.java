
package com.piphunter.ai;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiVisionAnalyzer {

    private static final String MODEL = "gemini-2.5-flash";

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent";

    public ChartResult analyze(Bitmap bitmap, String apiKey)
            throws Exception {

        if (bitmap == null) {
            throw new Exception("No chart screenshot selected.");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("Gemini API key is missing.");
        }

        ByteArrayOutputStream imageStream =
                new ByteArrayOutputStream();

        bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                82,
                imageStream
        );

        String imageBase64 = Base64.encodeToString(
                imageStream.toByteArray(),
                Base64.NO_WRAP
        );

        String prompt =
                "You are PipHunter AI, a careful forex chart analyst. "
                + "Analyze the attached trading chart screenshot. "
                + "Identify the instrument and timeframe only if visible. "
                + "Study trend, price action, swing structure, support, "
                + "resistance and readable indicators. "
                + "Choose BUY, SELL or WAIT. "
                + "Never invent prices or indicators. "
                + "Only give BUY or SELL when the screenshot provides "
                + "clear technical evidence and a readable price scale. "
                + "Otherwise choose WAIT and explain why. "
                + "For BUY or SELL provide entry, stop loss, TP1 and TP2 "
                + "based on visible price levels. "
                + "Confidence must reflect evidence, not guarantee success. "
                + "Return ONLY valid JSON with these string keys: "
                + "signal, instrument, timeframe, entry, stopLoss, "
                + "takeProfit1, takeProfit2, confidence, explanation. "
                + "Use an em dash for unavailable values.";

        JSONObject inlineData = new JSONObject();
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", imageBase64);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inlineData", inlineData);

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);
        parts.put(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject config = new JSONObject();
        config.put("responseMimeType", "application/json");
        config.put("temperature", 0.2);

        JSONObject request = new JSONObject();
        request.put("contents", contents);
        request.put("generationConfig", config);

        HttpURLConnection connection = null;

        try {
            URL url = new URL(ENDPOINT);

            connection = (HttpURLConnection)
                    url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "x-goog-api-key",
                    apiKey.trim()
            );

            byte[] body = request.toString()
                    .getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(body.length);

            try (OutputStream output =
                         connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();

            InputStream responseStream =
                    status >= 200 && status < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String responseText = readAll(responseStream);

            if (status < 200 || status >= 300) {
                throw new Exception(
                        explainApiError(status, responseText)
                );
            }

            JSONObject response =
                    new JSONObject(responseText);

            JSONArray candidates =
                    response.optJSONArray("candidates");

            if (candidates == null ||
                    candidates.length() == 0) {
                throw new Exception(
                        "Gemini returned no analysis."
                );
            }

            JSONObject candidate =
                    candidates.getJSONObject(0);

            JSONObject responseContent =
                    candidate.optJSONObject("content");

            JSONArray responseParts =
                    responseContent == null
                            ? null
                            : responseContent.optJSONArray("parts");

            String generatedText = "";

            if (responseParts != null) {
                for (int i = 0;
                     i < responseParts.length(); i++) {

                    generatedText += responseParts
                            .getJSONObject(i)
                            .optString("text", "");
                }
            }

            if (generatedText.trim().isEmpty()) {
                throw new Exception(
                        "Gemini returned an empty response."
                );
            }

            JSONObject resultJson =
                    new JSONObject(generatedText.trim());

            String signal = resultJson
                    .optString("signal", "WAIT")
                    .trim()
                    .toUpperCase();

            if (!signal.equals("BUY") &&
                    !signal.equals("SELL")) {
                signal = "WAIT";
            }

            String explanation =
                    "ONLINE GEMINI AI ANALYSIS\n"
                    + "Instrument: "
                    + resultJson.optString(
                            "instrument", "Not identified")
                    + "\nTimeframe: "
                    + resultJson.optString(
                            "timeframe", "Not identified")
                    + "\n\n"
                    + resultJson.optString(
                            "explanation",
                            "No explanation provided.")
                    + "\n\nEducational analysis only. "
                    + "Verify all levels before trading.";

            String entry = resultJson.optString(
                    "entry", "—");

            String stop = resultJson.optString(
                    "stopLoss", "—");

            String tp1 = resultJson.optString(
                    "takeProfit1", "—");

            String tp2 = resultJson.optString(
                    "takeProfit2", "—");

            String confidence = resultJson.optString(
                    "confidence", "Not calculated");

            if (signal.equals("WAIT")) {
                entry = "—";
                stop = "—";
                tp1 = "—";
                tp2 = "—";
            }

            return new ChartResult(
                    signal,
                    explanation,
                    entry,
                    stop,
                    tp1,
                    tp2,
                    confidence
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readAll(InputStream stream)
            throws Exception {

        if (stream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     stream,
                                     StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    private String explainApiError(
            int status,
            String body) {

        try {
            JSONObject json = new JSONObject(body);

            JSONObject error =
                    json.optJSONObject("error");

            String message = error == null
                    ? body
                    : error.optString("message", body);

            if (status == 400) {
                return "Gemini request error: " + message;
            }

            if (status == 401 || status == 403) {
                return "Gemini API key invalid or access denied: "
                        + message;
            }

            if (status == 429) {
                return "Gemini usage limit reached. Try again later: "
                        + message;
            }

            return "Gemini API error (" + status + "): "
                    + message;

        } catch (Exception ignored) {
            return "Gemini API error (" + status + ").";
        }
    }
}
