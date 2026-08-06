package com.flickpick.rankingservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

// Converts a rank position into a 0.0-5.0 score via percentile-to-tier
// bucketing rather than a straight linear mapping, so the middle of
// someone's list still lands in a respectable range instead of a
// mathematically "correct" but harsh 2.5. See docs/build-log for the
// reasoning and the alternative (pure linear) that was rejected.
@Component
public class ScoreCalculator {

    private record Band(double minPercentile, BigDecimal rangeLow, BigDecimal rangeHigh) {
    }

    // Ordered highest percentile threshold first.
    private static final Band[] BANDS = {
            new Band(0.90, new BigDecimal("4.6"), new BigDecimal("5.0")),
            new Band(0.70, new BigDecimal("4.0"), new BigDecimal("4.5")),
            new Band(0.40, new BigDecimal("3.0"), new BigDecimal("3.9")),
            new Band(0.15, new BigDecimal("2.0"), new BigDecimal("2.9")),
            new Band(0.00, new BigDecimal("0.0"), new BigDecimal("1.9")),
    };

    public BigDecimal scoreForPosition(int rankPosition, int totalRanked) {
        double percentile = percentileOf(rankPosition, totalRanked);

        for (int i = 0; i < BANDS.length; i++) {
            Band band = BANDS[i];
            if (percentile >= band.minPercentile()) {
                double bandTop = (i == 0) ? 1.0 : BANDS[i - 1].minPercentile();
                double bandSpan = bandTop - band.minPercentile();
                double withinBand = (bandSpan == 0) ? 1.0 : (percentile - band.minPercentile()) / bandSpan;

                BigDecimal rangeSpan = band.rangeHigh().subtract(band.rangeLow());
                return band.rangeLow()
                        .add(rangeSpan.multiply(BigDecimal.valueOf(withinBand)))
                        .setScale(1, RoundingMode.HALF_UP);
            }
        }
        throw new IllegalStateException("Unreachable: last band always matches (minPercentile 0.0)");
    }

    private double percentileOf(int rankPosition, int totalRanked) {
        if (totalRanked <= 1) {
            return 1.0;
        }
        // rankPosition is 0-indexed, 0 = most preferred.
        return (double) (totalRanked - 1 - rankPosition) / (totalRanked - 1);
    }
}
