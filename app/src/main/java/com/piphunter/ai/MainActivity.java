package com.piphunter.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private TextView cardText(String value, float size, int color) {
        TextView view = text(value, size, color);
        view.setPadding(dp(18), dp(14), dp(18), dp(14));
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int background = Color.rgb(8, 12, 24);
        final int card = Color.rgb(18, 25, 43);
        final int white = Color.WHITE;
        final int muted = Color.rgb(170, 180, 200);
        final int green = Color.rgb(50, 210, 120);
        final int red = Color.rgb(245, 80, 90);
        final int yellow = Color.rgb(255, 195, 70);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(background);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));

        TextView title = text("PipHunter AI", 30, white);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView subtitle = text(
                "AI Forex Market Scanner",
                15,
                muted
        );
        subtitle.setPadding(0, dp(3), 0, dp(22));

        root.addView(title);
        root.addView(subtitle);

        TextView status = cardText(
                "●  SCANNER ACTIVE",
                16,
                green
        );
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setBackgroundColor(card);

        root.addView(status);

        TextView market = cardText(
                "FOREX MARKET\nConnecting to live market data...",
                16,
                white
        );
        market.setBackgroundColor(card);

        root.addView(market);

        TextView pair = text("EUR/USD", 26, white);
        pair.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pair.setPadding(0, dp(28), 0, dp(5));

        root.addView(pair);

        TextView timeframe = text(
                "15 MINUTE ANALYSIS",
                13,
                muted
        );

        root.addView(timeframe);

        TextView signal = cardText(
                "LOADING...",
                26,
                yellow
        );

        signal.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        signal.setGravity(Gravity.CENTER);
        signal.setBackgroundColor(card);
        signal.setPadding(
                dp(18),
                dp(25),
                dp(18),
                dp(25)
        );

        root.addView(signal);

        TextView details = cardText(
                "Waiting for real market candles...",
                16,
                white
        );

        details.setBackgroundColor(card);

        root.addView(details);

        TextView engineStatus = text(
                "Analysis engine: ACTIVE",
                13,
                muted
        );

        engineStatus.setPadding(
                0,
                dp(22),
                0,
                0
        );

        root.addView(engineStatus);

        TextView disclaimer = text(
                "PipHunter AI analyzes market conditions and " +
                "provides trading signals. Signals are not financial advice.",
                12,
                muted
        );

        disclaimer.setPadding(
                0,
                dp(12),
                0,
                0
        );

        root.addView(disclaimer);

        scroll.addView(root);
        setContentView(scroll);

        FinnhubClient client = new FinnhubClient();

        client.getForexCandles(new FinnhubClient.Callback() {

            @Override
            public void onSuccess(String data) {

                try {
                    JSONObject json = new JSONObject(data);

                    if (!"ok".equalsIgnoreCase(
                            json.optString("s")
                    )) {
                        throw new Exception(
                                "Finnhub returned: "
                                        + json.optString("s")
                        );
                    }

                    JSONArray closes = json.getJSONArray("c");

                    if (closes.length() < 21) {
                        throw new Exception(
                                "Not enough market candles"
                        );
                    }

                    double[] prices = new double[closes.length()];

                    for (int i = 0; i < closes.length(); i++) {
                        prices[i] = closes.getDouble(i);
                    }

                    double price =
                            prices[prices.length - 1];

                    double fastAverage =
                            calculateSma(prices, 9);

                    double slowAverage =
                            calculateSma(prices, 21);

                    double rsi =
                            calculateRsi(prices, 14);

                    SignalEngine engine =
                            new SignalEngine();

                    SignalEngine.SignalResult result =
                            engine.analyze(
                                    price,
                                    fastAverage,
                                    slowAverage,
                                    rsi
                            );

                    runOnUiThread(() -> {

                        int signalColor = yellow;

                        if (result.signal.equals("BUY")) {
                            signalColor = green;
                        } else if (result.signal.equals("SELL")) {
                            signalColor = red;
                        }

                        signal.setText(result.signal);
                        signal.setTextColor(signalColor);

                        String detailsText;

                        if (result.signal.equals("WAIT")) {

                            detailsText =
                                    "Current Price: "
                                            + formatPrice(result.entry)
                                            + "\n\n"
                                            + "RSI: "
                                            + formatNumber(rsi)
                                            + "\n\n"
                                            + "Confidence: "
                                            + result.confidence
                                            + "%";

                        } else {

                            detailsText =
                                    "Entry: "
                                            + formatPrice(result.entry)
                                            + "\n\n"
                                            + "Stop Loss: "
                                            + formatPrice(result.stopLoss)
                                            + "\n\n"
                                            + "Take Profit 1: "
                                            + formatPrice(result.takeProfit1)
                                            + "\n\n"
                                            + "Take Profit 2: "
                                            + formatPrice(result.takeProfit2)
                                            + "\n\n"
                                            + "RSI: "
                                            + formatNumber(rsi)
                                            + "\n\n"
                                            + "Confidence: "
                                            + result.confidence
                                            + "%";
                        }

                        details.setText(detailsText);

                        market.setText(
                                "FOREX MARKET\n" +
                                "Live EUR/USD data received"
                        );
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        signal.setText("DATA ERROR");
                        signal.setTextColor(red);

                        details.setText(
                                "Unable to analyze live market data.\n\n"
                                        + e.getMessage()
                        );

                        market.setText(
                                "FOREX MARKET\n" +
                                "Live data connection failed"
                        );
                    });
                }
            }

            @Override
            public void onError(String error) {

                runOnUiThread(() -> {

                    signal.setText("DATA ERROR");
                    signal.setTextColor(red);

                    details.setText(
                            "Finnhub connection failed.\n\n"
                                    + error
                    );

                    market.setText(
                            "FOREX MARKET\n" +
                            "Live data connection failed"
                    );
                });
            }
        });
    }

    private double calculateSma(
            double[] prices,
            int period
    ) {

        if (prices.length < period) {
            return prices[prices.length - 1];
        }

        double sum = 0;

        for (
                int i = prices.length - period;
                i < prices.length;
                i++
        ) {
            sum += prices[i];
        }

        return sum / period;
    }

    private double calculateRsi(
            double[] prices,
            int period
    ) {

        if (prices.length <= period) {
            return 50.0;
        }

        double gains = 0;
        double losses = 0;

        int start = prices.length - period;

        for (int i = start; i < prices.length; i++) {

            double change =
                    prices[i] - prices[i - 1];

            if (change > 0) {
                gains += change;
            } else {
                losses += Math.abs(change);
            }
        }

        double averageGain = gains / period;
        double averageLoss = losses / period;

        if (averageLoss == 0) {
            return 100.0;
        }

        double relativeStrength =
                averageGain / averageLoss;

        return 100.0 -
                (100.0 / (1.0 + relativeStrength));
    }

    private String formatPrice(double value) {
        return String.format(
                java.util.Locale.US,
                "%.5f",
                value
        );
    }

    private String formatNumber(double value) {
        return String.format(
                java.util.Locale.US,
                "%.1f",
                value
        );
    }
}