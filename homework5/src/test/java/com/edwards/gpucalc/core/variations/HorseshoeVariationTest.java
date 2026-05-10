package com.edwards.gpucalc.core.variations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HorseshoeVariationTest {

    private final HorseshoeVariation v = new HorseshoeVariation();
    private static final double EPS = 1e-12;

    @Test
    void originMapsToOriginByConvention() {
        double[] out = new double[2];
        v.apply(0, 0, out);
        assertThat(out[0]).isCloseTo(0.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void unitXMapsToUnitX() {
        double[] out = new double[2];
        v.apply(1, 0, out);
        assertThat(out[0]).isCloseTo(1.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void unitYMapsToNegativeUnitX() {
        double[] out = new double[2];
        v.apply(0, 1, out);
        assertThat(out[0]).isCloseTo(-1.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void threeFourMapsToKnownPoint() {
        double[] out = new double[2];
        v.apply(3.0, 4.0, out);
        assertThat(out[0]).isCloseTo(-7.0 / 5.0, within(EPS));
        assertThat(out[1]).isCloseTo(24.0 / 5.0, within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
