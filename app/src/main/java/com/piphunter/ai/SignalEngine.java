package com.piphunter.ai;

public class SignalEngine {

    public static class SignalResult {
        public String signal;
        public int confidence;
        public double entry;
        public double stopLoss;
        public double takeProfit1;
        public double takeProfit2;

        public SignalResult(
                String signal,
                int confidence,
                double entry,
                double stopLoss,
                double takeProfit1,
                double takeProfit2
        ) {
            this.signal = signal;
            this.confidence = confidence;
            this.entry = entry;
            this.stopLoss = stopLoss;
            this.takeProfit1 = takeProfit1;
            this.takeProfit2 = takeProfit2;
        }
    }

    public SignalResult analyze(
            double price,
            double fastAverage,
            double slowAverage,
            double rsi
    ) {

        int buyScore = 0;
        int sellScore = 0;

        if (fastAverage > slowAverage) {
            buyScore++;
        }

        if (fastAverage < slowAverage) {
            sellScore++;
        }

        if (rsi >= 50 && rsi <= 70) {
            buyScore++;
        }

        if (rsi >= 30 && rsi < 50) {
            sellScore++;
        }

        if (buyScore > sellScore) {
            double stopLoss = price * 0.998;
            double tp1 = price * 1.003;
            double tp2 = price * 1.006;

            return new SignalResult(
                    "BUY",
                    70,
                    price,
                    stopLoss,
                    tp1,
                    tp2
            );
        }

        if (sellScore > buyScore) {
            double stopLoss = price * 1.002;
            double tp1 = price * 0.997;
            double tp2 = price * 0.994;

            return new SignalResult(
                    "SELL",
                    70,
                    price,
                    stopLoss,
                    tp1,
                    tp2
            );
        }

        return new SignalResult(
                "WAIT",
                40,
                price,
                0,
                0,
                0
        );
    }
}
