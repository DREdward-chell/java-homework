package com.edwards.gpucalc.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AffineCoeffsTest {

    private static final double EPS = 1e-12;

    @Test
    void identityPreservesPoint() {
        double[] out = new double[2];
        AffineCoeffs.identity().apply(3.5, -2.25, out);
        assertThat(out[0]).isCloseTo(3.5, within(EPS));
        assertThat(out[1]).isCloseTo(-2.25, within(EPS));
    }

    @Test
    void translationAddsConstants() {
        double[] out = new double[2];
        new AffineCoeffs(1, 0, 10, 0, 1, -5).apply(1, 1, out);
        assertThat(out[0]).isCloseTo(11.0, within(EPS));
        assertThat(out[1]).isCloseTo(-4.0, within(EPS));
    }

    @Test
    void scaleMultipliesAxes() {
        double[] out = new double[2];
        new AffineCoeffs(2, 0, 0, 0, 3, 0).apply(4, 5, out);
        assertThat(out[0]).isCloseTo(8.0, within(EPS));
        assertThat(out[1]).isCloseTo(15.0, within(EPS));
    }

    @Test
    void rotation90DegreesMapsXAxisToYAxis() {
        double[] out = new double[2];
        new AffineCoeffs(0, -1, 0, 1, 0, 0).apply(1, 0, out);
        assertThat(out[0]).isCloseTo(0.0, within(EPS));
        assertThat(out[1]).isCloseTo(1.0, within(EPS));
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
