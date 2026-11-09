
package com.piphunter.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FinnhubClient {

    public interface Callback {
        void onSuccess(String data);
        void onError(String error);
    }

    public void getForexCandles(Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = BuildConfig.FINNHUB_API_KEY;

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError(
                        "FINNHUB_API_KEY is missing. Add it in GitHub: " +
                        "Settings > Secrets and variables > Actions. " +
                        "Create a repository secret named FINNHUB_API_KEY, " +
                        "then rebuild the APK."
                    );
                    return;
                }

                // Finnhub requires Unix timestamps in seconds.
                long to = System.currentTimeMillis() / 1000L;
                long from = to - (7L * 24L * 60L * 60L);

                String encodedKey = URLEncoder.encode(
                    apiKey.trim(),
                    StandardCharsets.UTF_8.name()
                );

                String urlString =
                    "https://finnhub.io/api/v1/forex/candle" +
                    "?symbol=OANDA%3AEUR_USD" +
                    "&resolution=15" +
                    "&from=" + from +
                    "&to=" + to +
                    "&token=" + encodedKey;

                connection = (HttpURLConnection)
                    new URL(urlString).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty(
                    "Accept", "application/json"
                );
                connection.setRequestProperty(
                    "User-Agent", "PipHunter-AI-Android"
                );

                int code = connection.getResponseCode();

                InputStream stream =
                    code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                StringBuilder result = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                stream, StandardCharsets.UTF_8
                            )
                        )) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                    }
                }

                String body = result.toString();

                if (code >= 200 && code < 300) {
                    callback.onSuccess(body);
                } else {
                    callback.onError(
                        explainHttpError(code, body)
                    );
                }

            } catch (java.net.SocketTimeoutException e) {
                callback.onError(
                    "Connection timed out. Check your internet " +
                    "connection and try again."
                );

            } catch (java.net.UnknownHostException e) {
                callback.onError(
                    "Cannot reach Finnhub. Check your mobile data " +
                    "or Wi-Fi connection."
                );

            } catch (Exception e) {
                callback.onError(
                    "Connection error: " +
                    e.getClass().getSimpleName() +
                    (e.getMessage() == null
                        ? ""
                        : " - " + e.getMessage())
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String explainHttpError(int code, String body) {
        String message;

        switch (code) {
            case 401:
                message =
                    "Finnhub rejected the API key (HTTP 401). " +
                    "Check your FINNHUB_API_KEY GitHub secret.";
                break;

            case 403:
                message =
                    "Finnhub denied access to forex candles " +
                    "(HTTP 403). Your plan may not include this " +
                    "endpoint or symbol.";
                break;

            case 404:
                message =
                    "Finnhub could not find the endpoint or " +
                    "requested symbol (HTTP 404).";
                break;

            case 429:
                message =
                    "Finnhub rate limit reached (HTTP 429). " +
                    "Wait before trying again.";
                break;

            default:
                message = "Finnhub returned HTTP " + code + ".";
                break;
        }

        if (body != null && !body.trim().isEmpty()) {
            String detail = body.trim();

            if (detail.length() > 350) {
                detail = detail.substring(0, 350) + "...";
            }

            message += "\nServer response: " + detail;
        }

        return message;
    }
}
