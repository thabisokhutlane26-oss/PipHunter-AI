package com.piphunter.ai;

import android.graphics.Bitmap;

public class ChartAnalysisEngine {

    /*
     * First stage: validate the uploaded chart image.
     * No BUY or SELL signals are invented.
     */

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

        long pixels = (long) width * height;

        if (pixels < 30000L) {
            return ChartResult.waitForMoreData(
                    "Not enough image detail for safe chart analysis."
            );
        }

        return ChartResult.waitForMoreData(
                "Image validation successful: "
                + width + " × " + height
                + " pixels. Candle and price recognition "
                + "still needs a trained chart-vision model. "
                + "No trading signal or prices have been guessed."
        );
    }
}