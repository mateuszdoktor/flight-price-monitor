package com.flight_price_monitor.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class AnomalyDetector {
    private AnomalyDetector() {
    }

    public static BigDecimal calculateMean(List<BigDecimal> prices) {
        Objects.requireNonNull(prices, "Prices list is required");
        if (prices.isEmpty()) throw new IllegalArgumentException("Prices list can't be empty");

        int n = prices.size();
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateMedian(List<BigDecimal> prices) {
        Objects.requireNonNull(prices, "Prices list is required");
        if (prices.isEmpty()) throw new IllegalArgumentException("Prices list can't be empty");

        List<BigDecimal> sortedPrices = prices.stream().sorted().toList();
        int n = sortedPrices.size();
        if (n % 2 == 0) {
            BigDecimal x1 = sortedPrices.get(n / 2 - 1);
            BigDecimal x2 = sortedPrices.get(n / 2);
            return x1.add(x2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            return sortedPrices.get(n / 2);
        }
    }

    public static BigDecimal calculateStandardDeviation(List<BigDecimal> prices, BigDecimal mean) {
        Objects.requireNonNull(prices, "Prices list is required");
        Objects.requireNonNull(mean, "Mean value is required");
        if (prices.isEmpty()) throw new IllegalArgumentException("Prices list can't be empty");

        int n = prices.size();
        if (n == 1) return BigDecimal.ZERO;
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal sumOfSquares = (prices.stream()
                .map(x -> {
                    BigDecimal diff = x.subtract(mean);
                    return diff.pow(2, mc);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal variance = sumOfSquares.divide(BigDecimal.valueOf(n), mc);
        return variance.sqrt(mc).setScale(4, RoundingMode.HALF_UP);
    }

    public static Double calculateZScore(BigDecimal currentPrice, BigDecimal mean, BigDecimal stdDev) {
        Objects.requireNonNull(currentPrice, "Current price is required");
        Objects.requireNonNull(mean, "Mean value is required");
        Objects.requireNonNull(stdDev, "Standard deviation value is required");

        if (stdDev.equals(BigDecimal.ZERO)) return null;
        return currentPrice.subtract(mean).divide(stdDev, 4, RoundingMode.HALF_UP).doubleValue();
    }

    public static PriceStatistics buildStatistics(List<BigDecimal> historicalPrices, BigDecimal currentPrice) {
        Objects.requireNonNull(historicalPrices, "Historical prices list is required");
        Objects.requireNonNull(currentPrice, "Current price is required");
        if (historicalPrices.isEmpty()) throw new IllegalArgumentException("Prices list can't be empty");

        BigDecimal mean = calculateMean(historicalPrices);
        BigDecimal median = calculateMedian(historicalPrices);
        BigDecimal standardDeviation = calculateStandardDeviation(historicalPrices, mean);
        BigDecimal min = historicalPrices.stream().min(BigDecimal::compareTo).orElseThrow();
        BigDecimal max = historicalPrices.stream().max(BigDecimal::compareTo).orElseThrow();
        int sampleCount = historicalPrices.size();
        Double zScore = calculateZScore(currentPrice, mean, standardDeviation);

        return PriceStatistics.builder()
                .mean(mean)
                .median(median)
                .standardDeviation(standardDeviation)
                .min(min)
                .max(max)
                .sampleCount(sampleCount)
                .currentPrice(currentPrice)
                .zScore(zScore)
                .build();
    }

    public static boolean isAnomalyByZScore(Double zScore, double threshold) {
        return zScore != null && zScore < -threshold;
    }

    public static boolean isAnomalyByPercentage(BigDecimal currentPrice, BigDecimal mean, double threshold) {
        Objects.requireNonNull(currentPrice, "Current price is required");
        Objects.requireNonNull(mean, "Mean value is required");

        BigDecimal thresholdValue = mean.multiply(BigDecimal.valueOf(threshold));
        return currentPrice.compareTo(thresholdValue) <= 0;
    }
}