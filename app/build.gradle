package com.piphunter.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

                String urlString =
                        "https://finnhub.io/api/v1/forex/candle" +
                        "?symbol=OANDA:EUR_USD" +
                        "&resolution=15" +
                        "&count=50" +
                        "&token=" + apiKey;

                URL url = new URL(urlString);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();

                BufferedReader reader;

                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );
                } else {
                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream()
                            )
                    );
                }

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    callback.onSuccess(result.toString());
                } else {
                    callback.onError(
                            "HTTP " + responseCode + ": " + result
                    );
                }

            } catch (Exception e) {
                callback.onError(
                        e.getClass().getSimpleName()
                                + ": "
                                + e.getMessage()
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}