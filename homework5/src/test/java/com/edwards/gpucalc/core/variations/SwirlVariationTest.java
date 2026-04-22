package com.edwards.gpucalc.core.variations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwirlVariationTest {

    private final SwirlVariation v = new SwirlVariation();
    private static final double EPS = 1e-12;

    @Test
    void originMapsToOrigin() {
        double[] out = new double[2];
        v.apply(0, 0, out);
        assertThat(out[0]).isCloseTo(0.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void unitXMatchesClosedForm() {
        double[] out = new double[2];
        v.apply(1.0, 0.0, out);
        assertThat(out[0]).isCloseTo(Math.sin(1.0), within(EPS));
        assertThat(out[1]).isCloseTo(Math.cos(1.0), within(EPS));
    }

    @Test
    void unitYMatchesClosedForm() {
        double[] out = new double[2];
        v.apply(0.0, 1.0, out);
        assertThat(out[0]).isCloseTo(-Math.cos(1.0), within(EPS));
        assertThat(out[1]).isCloseTo(Math.sin(1.0), within(EPS));
    }

    @Test
    void diagonalPointMatchesClosedForm() {
        double[] out = new double[2];
        double x = 1.0, y = 1.0, r2 = 2.0;
        v.apply(x, y, out);
        assertThat(out[0]).isCloseTo(x * Math.sin(r2) - y * Math.cos(r2), within(EPS));
        assertThat(out[1]).isCloseTo(x * Math.cos(r2) + y * Math.sin(r2), within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
