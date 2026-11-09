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

        int activeColor = 0;
        int groupStartX = -1;
        int groupEndX = -1;

        int groupTop = height;
        int groupBottom = -1;

        int blankColumns = 0;

        for (int x = startX; x < endX; x++) {

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

            int columnColor = 0;
            int columnTop = height;
            int columnBottom = -1;

            if (greenCount >= 2 &&
                    greenCount > redCount * 1.35) {

                columnColor = 1;
                columnTop = greenTop;
                columnBottom = greenBottom;

            } else if (redCount >= 2 &&
                    redCount > greenCount * 1.35) {

                columnColor = 2;
                columnTop = redTop;
                columnBottom = redBottom;
            }

            if (columnColor == 0) {

                blankColumns++;

                if (blankColumns > 2 && activeColor != 0) {

                    if (groupEndX - groupStartX >= 1 &&
                            groupBottom - groupTop >= 4) {

                        if (activeColor == 1) {
                            bullish++;
                        } else {
                            bearish++;
                        }
                    }

                    activeColor = 0;
                    groupStartX = -1;
                    groupEndX = -1;
                    groupTop = height;
                    groupBottom = -1;
                }

                continue;
            }

            blankColumns = 0;

            if (activeColor == 0) {

                activeColor = columnColor;
                groupStartX = x;
                groupEndX = x;
                groupTop = columnTop;
                groupBottom = columnBottom;

            } else if (activeColor == columnColor) {

                groupEndX = x;
                groupTop = Math.min(groupTop, columnTop);
                groupBottom = Math.max(
                        groupBottom,
                        columnBottom
                );

            } else {

                if (groupEndX - groupStartX >= 1 &&
                        groupBottom - groupTop >= 4) {

                    if (activeColor == 1) {
                        bullish++;
                    } else {
                        bearish++;
                    }
                }

                activeColor = columnColor;
                groupStartX = x;
                groupEndX = x;
                groupTop = columnTop;
                groupBottom = columnBottom;
            }
        }

        // Finish the final group.
        if (activeColor != 0 &&
                groupEndX - groupStartX >= 1 &&
                groupBottom - groupTop >= 4) {

            if (activeColor == 1) {
                bullish++;
            } else {
                bearish++;
            }
        }

        int total = bullish + bearish;

        String explanation;

        if (total == 0) {

            explanation =
                    "No clear coloured candle candidates detected.\n\n"
                    + "Try a screenshot with larger, clearer candles.";

        } else {

            explanation =
                    "PRELIMINARY CANDLE GROUPING\n\n"
                    + "Possible bullish groups: " + bullish + "\n"
                    + "Possible bearish groups: " + bearish + "\n"
                    + "Total possible groups: " + total + "\n\n"
                    + "Coloured pixel columns were grouped "
                    + "into possible candle-like shapes.\n\n"
                    + "These are not verified candlesticks "
                    + "or confirmed market direction.";
        }

        return new CandleStats(
                bullish,
                bearish,
                explanation
        );
    }
}