package com.edwards.gpucalc.core.variations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinearVariationTest {

    private final LinearVariation v = new LinearVariation();
    private static final double EPS = 1e-12;

    @Test
    void identityAtOrigin() {
        double[] out = new double[2];
        v.apply(0, 0, out);
        assertThat(out[0]).isCloseTo(0.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void identityAtUnitPoint() {
        double[] out = new double[2];
        v.apply(1, 0, out);
        assertThat(out[0]).isCloseTo(1.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void identityAtArbitraryPoint() {
        double[] out = new double[2];
        v.apply(3.7, -2.1, out);
        assertThat(out[0]).isCloseTo(3.7, within(EPS));
        assertThat(out[1]).isCloseTo(-2.1, within(EPS));
    }

    @Test
    void identityAtNegativePoint() {
        double[] out = new double[2];
        v.apply(-5.0, -5.0, out);
        assertThat(out[0]).isCloseTo(-5.0, within(EPS));
        assertThat(out[1]).isCloseTo(-5.0, within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
