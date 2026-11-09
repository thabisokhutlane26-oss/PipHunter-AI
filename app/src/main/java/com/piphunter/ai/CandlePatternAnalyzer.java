package com.piphunter.ai;

import android.graphics.Bitmap;
import android.graphics.Color;

public class CandlePatternAnalyzer {

    public static class CandleStats {
        public final int bullishCandidates;
        public final int bearishCandidates;
        public final int totalCandidates;
        public final String explanation;

        public CandleStats(
                int bullish,
                int bearish,
                String explanation) {

            this.bullishCandidates = bullish;
            this.bearishCandidates = bearish;
            this.totalCandidates = bullish + bearish;
            this.explanation = explanation;
        }
    }

    public CandleStats analyze(Bitmap bitmap) {

        if (bitmap == null) {
            return new CandleStats(
                    0, 0,
                    "No screenshot available."
            );
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width < 200 || height < 120) {
            return new CandleStats(
                    0, 0,
                    "Chart image is too small."
            );
        }

        int startX = width / 12;
        int endX = width - width / 12;

        int startY = height / 6;
        int endY = height - height / 7;

        int bullish = 0;
        int bearish = 0;

        int lastDetectedX = -100;

        for (int x = startX; x < endX; x += 2) {

            int greenCount = 0;
            int redCount = 0;

            int greenTop = height;
            int greenBottom = -1;

            int redTop = height;
            int redBottom = -1;

            for (int y = startY; y < endY; y += 2) {

                int pixel = bitmap.getPixel(x, y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                boolean green =
                        g > 90 &&
                        g > r * 1.18 &&
                        g > b * 0.90;

                boolean red =
                        r > 90 &&
                        r > g * 1.25 &&
                        r > b * 1.15;

                if (green) {
                    greenCount++;
                    greenTop = Math.min(greenTop, y);
                    greenBottom = Math.max(greenBottom, y);
                }

                if (red) {
                    redCount++;
                    redTop = Math.min(redTop, y);
                    redBottom = Math.max(redBottom, y);
                }
            }

            if (x - lastDetectedX < 5) {
                continue;
            }

            if (greenCount >= 3 && greenCount > redCount * 1.5) {

                int verticalSize = greenBottom - greenTop;

                if (verticalSize >= 4) {
                    bullish++;
                    lastDetectedX = x;
                }

            } else if (redCount >= 3 && redCount > greenCount * 1.5) {

                int verticalSize = redBottom - redTop;

                if (verticalSize >= 4) {
                    bearish++;
                    lastDetectedX = x;
                }
            }
        }

        int total = bullish + bearish;

        String explanation;

        if (total == 0) {
            explanation =
                    "No clear candle candidates detected. "
                    + "Try a screenshot with larger, clearer candles.";
        } else {
            explanation =
                    "Preliminary candle candidates detected.\n\n"
                    + "Possible bullish columns: " + bullish + "\n"
                    + "Possible bearish columns: " + bearish + "\n"
                    + "Total candidates: " + total + "\n\n"
                    + "These are pixel-based candidates, not verified "
                    + "candles or confirmed market direction.";
        }

        return new CandleStats(
                bullish,
                bearish,
                explanation
        );
    }
}