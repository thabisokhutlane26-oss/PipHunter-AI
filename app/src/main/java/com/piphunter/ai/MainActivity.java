package com.piphunter.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

        /*
         * TEST VALUES ONLY.
         * These are NOT live market prices.
         * They are used to verify that the SignalEngine
         * can analyze conditions and return a result.
         */
        double testPrice = 1.1000;
        double testFastAverage = 1.1010;
        double testSlowAverage = 1.0990;
        double testRsi = 58.0;

        SignalEngine engine = new SignalEngine();

        SignalEngine.SignalResult result = engine.analyze(
                testPrice,
                testFastAverage,
                testSlowAverage,
                testRsi
        );

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
                "FOREX MARKET\nScanning market conditions",
                16,
                white
        );
        market.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
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

        int signalColor = yellow;

        if (result.signal.equals("BUY")) {
            signalColor = green;
        } else if (result.signal.equals("SELL")) {
            signalColor = red;
        }

        TextView signal = cardText(
                result.signal,
                26,
                signalColor
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

        String detailsText;

        if (result.signal.equals("WAIT")) {

            detailsText =
                    "Market conditions are not strong enough.\n\n" +
                    "Confidence: " + result.confidence + "%";

        } else {

            detailsText =
                    "Entry: " + result.entry + "\n\n" +
                    "Stop Loss: " + result.stopLoss + "\n\n" +
                    "Take Profit 1: " + result.takeProfit1 + "\n\n" +
                    "Take Profit 2: " + result.takeProfit2 + "\n\n" +
                    "Confidence: " + result.confidence + "%";
        }

        TextView details = cardText(
                detailsText,
                16,
                white
        );

        details.setPadding(
                dp(18),
                dp(20),
                dp(18),
                dp(20)
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
    }
}x
