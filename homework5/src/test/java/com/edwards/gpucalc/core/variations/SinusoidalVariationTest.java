package com.edwards.gpucalc.core.variations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SinusoidalVariationTest {

    private final SinusoidalVariation v = new SinusoidalVariation();
    private static final double EPS = 1e-12;

    @Test
    void originMapsToOrigin() {
        double[] out = new double[2];
        v.apply(0, 0, out);
        assertThat(out[0]).isCloseTo(0.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void piOverTwoMapsToOne() {
        double[] out = new double[2];
        v.apply(Math.PI / 2, Math.PI / 2, out);
        assertThat(out[0]).isCloseTo(1.0, within(EPS));
        assertThat(out[1]).isCloseTo(1.0, within(EPS));
    }

    @Test
    void piMapsApproximatelyToZero() {
        double[] out = new double[2];
        v.apply(Math.PI, Math.PI, out);
        assertThat(out[0]).isCloseTo(0.0, within(1e-10));
        assertThat(out[1]).isCloseTo(0.0, within(1e-10));
    }

    @Test
    void asymmetricPoint() {
        double[] out = new double[2];
        v.apply(0.5, -0.5, out);
        assertThat(out[0]).isCloseTo(Math.sin(0.5), within(EPS));
        assertThat(out[1]).isCloseTo(Math.sin(-0.5), within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
