package com.edwards.gpucalc.core.variations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SphericalVariationTest {

    private final SphericalVariation v = new SphericalVariation();
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
    void pointOutsideUnitCircleMovesInside() {
        double[] out = new double[2];
        v.apply(2.0, 0.0, out);
        assertThat(out[0]).isCloseTo(0.5, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void diagonalPointInversion() {
        double[] out = new double[2];
        v.apply(3.0, 4.0, out);
        double r2 = 25.0;
        assertThat(out[0]).isCloseTo(3.0 / r2, within(EPS));
        assertThat(out[1]).isCloseTo(4.0 / r2, within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
