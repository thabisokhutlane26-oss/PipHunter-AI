
package com.piphunter.ai;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ChartAnalysisEngine {

    private final CandlePatternAnalyzer candleAnalyzer =
            new CandlePatternAnalyzer();

    public ChartResult analyze(Bitmap bitmap) {

        if (bitmap == null) {
            return ChartResult.waitForMoreData(
                    "No chart image was provided. Please select a screenshot."
            );
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width < 200 || height < 120) {
            return ChartResult.waitForMoreData(
                    "Screenshot is too small. Choose a clearer chart."
            );
        }

        // Run preliminary candle candidate analysis.
        CandlePatternAnalyzer.CandleStats candleStats =
                candleAnalyzer.analyze(bitmap);

        long greenPixels = 0;
        long redPixels = 0;

        boolean[] greenColumns = new boolean[width];
        boolean[] redColumns = new boolean[width];

        int startX = width / 12;
        int endX = width - (width / 12);

        int startY = height / 6;
        int endY = height - (height / 7);

        // Inspect the main chart area.

        for (int y = startY; y < endY; y += 2) {

            for (int x = startX; x < endX; x += 2) {

                int pixel = bitmap.getPixel(x, y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                boolean isGreen =
                        g > 90 &&
                        g > r * 1.18 &&
                        g > b * 0.90;

                boolean isRed =
                        r > 90 &&
                        r > g * 1.25 &&
                        r > b * 1.15;

                if (isGreen) {
                    greenPixels++;
                    greenColumns[x] = true;
                }

                if (isRed) {
                    redPixels++;
                    redColumns[x] = true;
                }
            }
        }

        if (greenPixels + redPixels < 40) {

            String explanation =
                    "PRELIMINARY IMAGE ANALYSIS\n\n"
                    + "Image dimensions: "
                    + width + " × " + height + " pixels\n\n"
                    + "Clear green or red chart pixels "
                    + "could not be detected.\n\n"
                    + candleStats.explanation
                    + "\n\nNo trading direction can be confirmed.";

            return ChartResult.waitForMoreData(explanation);
        }

        int greenColumnsFound = 0;
        int redColumnsFound = 0;

        for (int x = startX; x < endX; x++) {

            if (greenColumns[x]) {
                greenColumnsFound++;
            }

            if (redColumns[x]) {
                redColumnsFound++;
            }
        }

        long total = greenPixels + redPixels;

        int greenPercent =
                (int) ((greenPixels * 100L) / total);

        int redPercent = 100 - greenPercent;

        String explanation =
                "PRELIMINARY IMAGE ANALYSIS\n\n"
                + "Image dimensions: "
                + width + " × " + height + " pixels\n\n"
                + "Green pixel share: "
                + greenPercent + "%\n"
                + "Red pixel share: "
                + redPercent + "%\n\n"
                + "Green-active image columns: "
                + greenColumnsFound + "\n"
                + "Red-active image columns: "
                + redColumnsFound + "\n\n"
                + "CANDLE CANDIDATE ANALYSIS\n\n"
                + candleStats.explanation
                + "\n\n"
                + "IMPORTANT\n"
                + "These are preliminary pixel measurements. "
                + "They do not confirm real candle patterns, "
                + "market direction, entry prices or trading signals.";

        return ChartResult.waitForMoreData(explanation);
    }
}
