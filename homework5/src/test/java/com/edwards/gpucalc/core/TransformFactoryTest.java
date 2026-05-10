package com.edwards.gpucalc.core;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class TransformFactoryTest {

    @Autowired
    TransformFactory factory;

    @Test
    void buildsOneTransformPerAffine() {
        ResolvedConfig cfg = cfgWith(
                List.of(AffineCoeffs.identity(), new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0)),
                List.of(new WeightedVariationRef("linear", 1.0)));
        Transform[] transforms = factory.build(cfg);
        assertThat(transforms).hasSize(2);
    }

    @Test
    void colorCoordsSpreadEvenlyAcrossUnitInterval() {
        ResolvedConfig cfg = cfgWith(
                List.of(AffineCoeffs.identity(), AffineCoeffs.identity(), AffineCoeffs.identity()),
                List.of(new WeightedVariationRef("linear", 1.0)));
        Transform[] transforms = factory.build(cfg);
        assertThat(transforms[0].colorCoord()).isEqualTo(0.0);
        assertThat(transforms[1].colorCoord()).isEqualTo(0.5);
        assertThat(transforms[2].colorCoord()).isEqualTo(1.0);
    }

    @Test
    void singleAffineGetsMidpointColorCoord() {
        ResolvedConfig cfg = cfgWith(
                List.of(AffineCoeffs.identity()),
                List.of(new WeightedVariationRef("swirl", 1.0)));
        Transform[] transforms = factory.build(cfg);
        assertThat(transforms).hasSize(1);
        assertThat(transforms[0].colorCoord()).isEqualTo(0.5);
    }

    @Test
    void unknownVariationRaisesIllegalArgument() {
        ResolvedConfig cfg = cfgWith(
                List.of(AffineCoeffs.identity()),
                List.of(new WeightedVariationRef("not-a-variation", 1.0)));
        assertThatThrownBy(() -> factory.build(cfg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blendAppliedToAllTransforms() {
        ResolvedConfig cfg = cfgWith(
                List.of(AffineCoeffs.identity(), AffineCoeffs.identity()),
                List.of(new WeightedVariationRef("swirl", 0.5),
                        new WeightedVariationRef("linear", 0.5)));
        Transform[] transforms = factory.build(cfg);
        assertThat(transforms[0].variationCount()).isEqualTo(2);
        assertThat(transforms[1].variationCount()).isEqualTo(2);
    }

    private static ResolvedConfig cfgWith(List<AffineCoeffs> affines, List<WeightedVariationRef> fns) {
        return new ResolvedConfig(100, 100, 1.0, 100, "out.png", 1, affines, fns);
    }
}
