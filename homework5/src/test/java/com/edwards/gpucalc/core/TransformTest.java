package com.edwards.gpucalc.core;

import com.edwards.gpucalc.core.variations.LinearVariation;
import com.edwards.gpucalc.core.variations.SwirlVariation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformTest {

    private static final double EPS = 1e-12;

    @Test
    void identityAffineLinearVariationIsIdentity() {
        Transform t = new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation()},
                new double[]{1.0},
                0.5);
        double[] scratch = new double[2];
        double[] out = new double[2];
        t.apply(3.0, -2.0, scratch, out);
        assertThat(out[0]).isCloseTo(3.0, within(EPS));
        assertThat(out[1]).isCloseTo(-2.0, within(EPS));
    }

    @Test
    void weightScalesVariationOutput() {
        Transform t = new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation()},
                new double[]{0.5},
                0.0);
        double[] scratch = new double[2];
        double[] out = new double[2];
        t.apply(4.0, 8.0, scratch, out);
        assertThat(out[0]).isCloseTo(2.0, within(EPS));
        assertThat(out[1]).isCloseTo(4.0, within(EPS));
    }

    @Test
    void affineIsAppliedBeforeVariations() {
        AffineCoeffs shiftX = new AffineCoeffs(1, 0, 5, 0, 1, 0);
        Transform t = new Transform(
                shiftX,
                new Variation[]{new LinearVariation()},
                new double[]{1.0},
                0.25);
        double[] scratch = new double[2];
        double[] out = new double[2];
        t.apply(0, 0, scratch, out);
        assertThat(out[0]).isCloseTo(5.0, within(EPS));
        assertThat(out[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void blendOfTwoVariations() {
        Transform t = new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation(), new SwirlVariation()},
                new double[]{0.5, 0.5},
                0.1);
        double[] scratch = new double[2];
        double[] out = new double[2];
        t.apply(1.0, 0.0, scratch, out);
        double expectedX = 0.5 * (Math.sin(1.0) + 1.0);
        double expectedY = 0.5 * Math.cos(1.0);
        assertThat(out[0]).isCloseTo(expectedX, within(EPS));
        assertThat(out[1]).isCloseTo(expectedY, within(EPS));
    }

    @Test
    void rejectsLengthMismatch() {
        assertThatThrownBy(() -> new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation()},
                new double[]{1.0, 0.5},
                0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyVariations() {
        assertThatThrownBy(() -> new Transform(
                AffineCoeffs.identity(),
                new Variation[0],
                new double[0],
                0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOutOfRangeColorCoord() {
        assertThatThrownBy(() -> new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation()},
                new double[]{1.0},
                1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accessorsReturnConstructorValues() {
        Transform t = new Transform(
                AffineCoeffs.identity(),
                new Variation[]{new LinearVariation(), new SwirlVariation()},
                new double[]{0.7, 0.3},
                0.42);
        assertThat(t.affine()).isEqualTo(AffineCoeffs.identity());
        assertThat(t.colorCoord()).isEqualTo(0.42);
        assertThat(t.variationCount()).isEqualTo(2);
    }

    private static org.assertj.core.data.Offset<Double> within(double e) {
        return org.assertj.core.data.Offset.offset(e);
    }
}
