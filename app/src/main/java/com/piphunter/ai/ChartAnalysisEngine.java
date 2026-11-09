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

        int startX = width / 12;
        int endX = width - width / 12;
        int startY = height / 6;
        int endY = height - height / 7;

        // Check visual detail in the main chart area.
        long luminanceSum = 0;
        long luminanceSquareSum = 0;
        long sampleCount = 0;
        long colouredSamples = 0;

        for (int y = startY; y < endY; y += 4) {

            for (int x = startX; x < endX; x += 4) {

                int pixel = bitmap.getPixel(x, y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                int luminance =
                        (int) (0.299 * r + 0.587 * g + 0.114 * b);

                luminanceSum += luminance;
                luminanceSquareSum +=
                        (long) luminance * luminance;

                sampleCount++;

                boolean green =
                        g > 90 &&
                        g > r * 1.18 &&
                        g > b * 0.90;

                boolean red =
                        r > 90 &&
                        r > g * 1.25 &&
                        r > b * 1.15;

                if (green || red) {
                    colouredSamples++;
                }
            }
        }

        if (sampleCount == 0) {
            return ChartResult.waitForMoreData(
                    "The chart area could not be inspected."
            );
        }

        double averageBrightness =
                luminanceSum / (double) sampleCount;

        double brightnessVariance =
                luminanceSquareSum / (double) sampleCount
                - averageBrightness * averageBrightness;

        if (brightnessVariance < 0) {
            brightnessVariance = 0;
        }

        double colourRatio =
                colouredSamples / (double) sampleCount;

        String qualityDetails =
                "SCREENSHOT QUALITY CHECK\n\n"
                + "Image size: " + width + " × " + height + "\n"
                + "Average brightness: "
                + String.format("%.1f", averageBrightness) + "/255\n"
                + "Visual detail score: "
                + String.format("%.1f", brightnessVariance) + "\n"
                + "Red/green colour samples: "
                + String.format("%.2f", colourRatio * 100) + "%\n";

        // Reject images that are almost completely uniform
        // and contain practically no visible chart colours.
        if (brightnessVariance < 18 && colourRatio < 0.005) {

            return ChartResult.waitForMoreData(
                    qualityDetails
                    + "\nQUALITY RESULT: INSUFFICIENT VISUAL DETAIL\n\n"
                    + "The selected image appears nearly blank "
                    + "or has too little visible chart information.\n\n"
                    + "Please upload a clearer screenshot showing "
                    + "the chart candles and chart area."
            );
        }

        // Run preliminary candle candidate analysis.
        CandlePatternAnalyzer.CandleStats candleStats =
                candleAnalyzer.analyze(bitmap);

        long greenPixels = 0;
        long redPixels = 0;

        boolean[] greenColumns = new boolean[width];
        boolean[] redColumns = new boolean[width];

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
                    qualityDetails
                    + "\nQUALITY RESULT: LIMITED CHART COLOURS\n\n"
                    + "Clear green or red chart pixels "
                    + "could not be detected reliably.\n\n"
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
                qualityDetails
                + "\nQUALITY RESULT: BASIC VISUAL DETAIL DETECTED\n\n"
                + "PRELIMINARY IMAGE ANALYSIS\n\n"
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
                + "\n\nIMPORTANT\n"
                + "These are preliminary pixel measurements. "
                + "They do not confirm real candle patterns, "
                + "market direction, entry prices or trading signals.";

        return ChartResult.waitForMoreData(explanation);
    }
}