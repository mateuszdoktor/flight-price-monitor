package com.flight_price_monitor.domain.logic;

import com.flight_price_monitor.domain.model.AnomalyDetector;
import com.flight_price_monitor.domain.model.PriceStatistics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnomalyDetectorTest {

    // calculateMean

    @Test
    void calculateMean_withValidPrices_returnsCorrectMean() {
        assertEquals(new BigDecimal("200.00"), AnomalyDetector.calculateMean(
                List.of(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("300.00"))));
    }

    @Test
    void calculateMean_withEmptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> AnomalyDetector.calculateMean(List.of()));
    }

    @Test
    void calculateMean_withSingleElement_returnsThatElement() {
        assertEquals(new BigDecimal("150.00"), AnomalyDetector.calculateMean(List.of(new BigDecimal("150.00"))));
    }

    // calculateMedian

    @Test
    void calculateMedian_withOddCount_returnsMiddle() {
        BigDecimal result = AnomalyDetector.calculateMedian(
                List.of(new BigDecimal("300.00"), new BigDecimal("100.00"), new BigDecimal("200.00")));
        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    void calculateMedian_withEvenCount_returnsAvgOfMiddleTwo() {
        BigDecimal result = AnomalyDetector.calculateMedian(
                List.of(new BigDecimal("400.00"), new BigDecimal("100.00"),
                        new BigDecimal("300.00"), new BigDecimal("200.00")));
        assertEquals(new BigDecimal("250.00"), result);
    }

    @Test
    void calculateMedian_withEmptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> AnomalyDetector.calculateMedian(List.of()));
    }

    @Test
    void calculateMedian_withSingleElement_returnsThatElement() {
        assertEquals(new BigDecimal("99.00"), AnomalyDetector.calculateMedian(List.of(new BigDecimal("99.00"))));
    }

    // calculateStandardDeviation

    @Test
    void calculateStandardDeviation_withMultiplePrices_returnsCorrectValue() {
        BigDecimal mean = new BigDecimal("200.00");
        BigDecimal result = AnomalyDetector.calculateStandardDeviation(
                List.of(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("300.00")), mean);
        assertEquals(new BigDecimal("81.6497"), result);
    }

    @Test
    void calculateStandardDeviation_withSingleElement_returnsZero() {
        BigDecimal result = AnomalyDetector.calculateStandardDeviation(
                List.of(new BigDecimal("500.00")), new BigDecimal("500.00"));
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateStandardDeviation_withIdenticalPrices_returnsZero() {
        BigDecimal mean = new BigDecimal("200.00");
        BigDecimal result = AnomalyDetector.calculateStandardDeviation(
                List.of(new BigDecimal("200.00"), new BigDecimal("200.00"), new BigDecimal("200.00")), mean);
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateStandardDeviation_withEmptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AnomalyDetector.calculateStandardDeviation(List.of(), BigDecimal.ZERO));
    }

    // calculateZScore

    @Test
    void calculateZScore_withNonZeroStdDev_returnsCorrectValue() {
        Double result = AnomalyDetector.calculateZScore(
                new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("100.00"));
        assertNotNull(result);
        assertEquals(1.0, result, 0.0001);
    }

    @Test
    void calculateZScore_withNegativeDeviation_returnsNegativeValue() {
        Double result = AnomalyDetector.calculateZScore(
                new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("100.00"));
        assertNotNull(result);
        assertEquals(-1.0, result, 0.0001);
    }

    @Test
    void calculateZScore_withZeroStdDev_returnsNull() {
        Double result = AnomalyDetector.calculateZScore(
                new BigDecimal("200.00"), new BigDecimal("200.00"), BigDecimal.ZERO);
        assertNull(result);
    }

    // buildStatistics

    @Test
    void buildStatistics_returnsCorrectAggregatedValues() {
        List<BigDecimal> prices = List.of(
                new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("300.00"));
        BigDecimal currentPrice = new BigDecimal("400.00");

        PriceStatistics stats = AnomalyDetector.buildStatistics(prices, currentPrice);

        assertEquals(new BigDecimal("200.00"), stats.mean());
        assertEquals(new BigDecimal("200.00"), stats.median());
        assertEquals(new BigDecimal("100.00"), stats.min());
        assertEquals(new BigDecimal("300.00"), stats.max());
        assertEquals(3, stats.sampleCount());
        assertEquals(currentPrice, stats.currentPrice());
        assertNotNull(stats.zScore());
    }

    @Test
    void buildStatistics_withEmptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AnomalyDetector.buildStatistics(List.of(), new BigDecimal("100.00")));
    }

    // isAnomalyByZScore

    @Test
    void isAnomalyByZScore_whenZScoreBelowNegativeThreshold_returnsTrue() {
        assertTrue(AnomalyDetector.isAnomalyByZScore(-3.0, 2.0));
    }

    @Test
    void isAnomalyByZScore_whenZScoreAboveNegativeThreshold_returnsFalse() {
        assertFalse(AnomalyDetector.isAnomalyByZScore(-1.0, 2.0));
    }

    @Test
    void isAnomalyByZScore_whenZScoreIsNull_returnsFalse() {
        assertFalse(AnomalyDetector.isAnomalyByZScore(null, 2.0));
    }

    @Test
    void isAnomalyByZScore_whenZScoreEqualsNegativeThreshold_returnsFalse() {
        assertFalse(AnomalyDetector.isAnomalyByZScore(-2.0, 2.0));
    }

    // isAnomalyByPercentage

    @Test
    void isAnomalyByPercentage_whenPriceAtThreshold_returnsTrue() {
        assertTrue(AnomalyDetector.isAnomalyByPercentage(
                new BigDecimal("70.00"), new BigDecimal("100.00"), 0.7));
    }

    @Test
    void isAnomalyByPercentage_whenPriceBelowThreshold_returnsTrue() {
        assertTrue(AnomalyDetector.isAnomalyByPercentage(
                new BigDecimal("60.00"), new BigDecimal("100.00"), 0.7));
    }

    @Test
    void isAnomalyByPercentage_whenPriceAboveThreshold_returnsFalse() {
        assertFalse(AnomalyDetector.isAnomalyByPercentage(
                new BigDecimal("80.00"), new BigDecimal("100.00"), 0.7));
    }
}
