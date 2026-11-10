
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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiVisionAnalyzer {

    private static final String MODEL = "gemini-3.8-flash";

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent";

    private static final int MAX_ATTEMPTS = 2;

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 120000;

    public ChartResult analyze(Bitmap bitmap, String apiKey)
            throws Exception {

        if (bitmap == null) {
            throw new Exception("No chart screenshot selected.");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("Gemini API key is missing.");
        }

        // Reduce image dimensions to make uploads faster.
        Bitmap uploadBitmap = bitmap;
        boolean createdScaledBitmap = false;

        int maxDimension = 1280;
        int largestSide = Math.max(
                bitmap.getWidth(),
                bitmap.getHeight()
        );

        if (largestSide > maxDimension) {

            float scale = (float) maxDimension / largestSide;

            int newWidth = Math.max(
                    1,
                    Math.round(bitmap.getWidth() * scale)
            );

            int newHeight = Math.max(
                    1,
                    Math.round(bitmap.getHeight() * scale)
            );

            uploadBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    newWidth,
                    newHeight,
                    true
            );

            createdScaledBitmap = uploadBitmap != bitmap;
        }

        String imageBase64;

        try {

            ByteArrayOutputStream imageStream =
                    new ByteArrayOutputStream();

            uploadBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    72,
                    imageStream
            );

            imageBase64 = Base64.encodeToString(
                    imageStream.toByteArray(),
                    Base64.NO_WRAP
            );

        } finally {

            if (createdScaledBitmap
                    && !uploadBitmap.isRecycled()) {
                uploadBitmap.recycle();
            }
        }

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

        JSONObject thinkingConfig = new JSONObject();
        thinkingConfig.put("thinkingLevel", "LOW");

        JSONObject config = new JSONObject();
        config.put("responseMimeType", "application/json");
        config.put("thinkingConfig", thinkingConfig);
        config.put("maxOutputTokens", 1024);

        JSONObject request = new JSONObject();
        request.put("contents", contents);
        request.put("generationConfig", config);

        String requestBody = request.toString();

        Exception lastError = null;

        for (int attempt = 1;
             attempt <= MAX_ATTEMPTS;
             attempt++) {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(ENDPOINT);

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "x-goog-api-key",
                        apiKey.trim()
                );

                byte[] body = requestBody.getBytes(
                        StandardCharsets.UTF_8
                );

                connection.setFixedLengthStreamingMode(
                        body.length
                );

                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(body);
                    output.flush();
                }

                int status = connection.getResponseCode();

                InputStream responseStream =
                        status >= 200 && status < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String responseText = readAll(responseStream);

                if (status == 429
                        || status == 503
                        || status == 408
                        || status >= 500) {

                    lastError = new Exception(
                            explainApiError(status, responseText)
                    );

                    if (attempt < MAX_ATTEMPTS) {
                        Thread.sleep(3000L);
                        continue;
                    }

                    throw lastError;
                }

                if (status < 200 || status >= 300) {
                    throw new Exception(
                            explainApiError(status, responseText)
                    );
                }

                JSONObject response =
                        new JSONObject(responseText);

                JSONArray candidates =
                        response.optJSONArray("candidates");

                if (candidates == null
                        || candidates.length() == 0) {

                    throw new Exception(
                            "Gemini returned no analysis. Try another chart."
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

                StringBuilder generatedText =
                        new StringBuilder();

                if (responseParts != null) {

                    for (int i = 0;
                         i < responseParts.length();
                         i++) {

                        generatedText.append(
                                responseParts.getJSONObject(i)
                                        .optString("text", "")
                        );
                    }
                }

                if (generatedText.toString().trim().isEmpty()) {
                    throw new Exception(
                            "Gemini returned an empty response."
                    );
                }

                JSONObject resultJson =
                        new JSONObject(
                                generatedText.toString().trim()
                        );

                String signal = resultJson
                        .optString("signal", "WAIT")
                        .trim()
                        .toUpperCase();

                if (!signal.equals("BUY")
                        && !signal.equals("SELL")) {
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

            } catch (SocketTimeoutException e) {

                lastError = new Exception(
                        "Gemini response timed out. "
                        + "Check your internet connection and try again."
                );

                if (attempt < MAX_ATTEMPTS) {
                    Thread.sleep(3000L);
                    continue;
                }

                throw lastError;

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new Exception(
                        "Gemini analysis was interrupted."
                );

            } catch (IOException e) {

                lastError = new Exception(
                        "Network connection failed: "
                                + e.getMessage()
                );

                if (attempt < MAX_ATTEMPTS) {
                    Thread.sleep(3000L);
                    continue;
                }

                throw lastError;

            } catch (Exception e) {

                throw e;

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        throw lastError != null
                ? lastError
                : new Exception("Gemini analysis failed.");
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

            if (status == 503) {
                return "Gemini is temporarily overloaded: "
                        + message;
            }

            return "Gemini API error (" + status + "): "
                    + message;

        } catch (Exception ignored) {

            return "Gemini API error (" + status + ").";
        }
    }
}
