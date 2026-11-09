package com.piphunter.ai;

public class ChartResult {

    public final String signal;
    public final String explanation;
    public final String entry;
    public final String stopLoss;
    public final String takeProfit1;
    public final String takeProfit2;
    public final String confidence;

    public ChartResult(
            String signal,
            String explanation,
            String entry,
            String stopLoss,
            String takeProfit1,
            String takeProfit2,
            String confidence) {

        this.signal = signal;
        this.explanation = explanation;
        this.entry = entry;
        this.stopLoss = stopLoss;
        this.takeProfit1 = takeProfit1;
        this.takeProfit2 = takeProfit2;
        this.confidence = confidence;
    }

    public static ChartResult waitForMoreData(String reason) {
        return new ChartResult(
                "WAIT",
                reason,
                "Not available",
                "Not available",
                "Not available",
                "Not available",
                "Not calculated"
        );
    }
}