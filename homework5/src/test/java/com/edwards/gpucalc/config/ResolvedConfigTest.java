package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedConfigTest {

    @Test
    void listFieldsAreDefensivelyCopied() {
        List<AffineCoeffs> af = new ArrayList<>(List.of(new AffineCoeffs(1, 0, 0, 0, 1, 0)));
        List<WeightedVariationRef> fn = new ArrayList<>(List.of(new WeightedVariationRef("linear", 1.0)));
        ResolvedConfig c = new ResolvedConfig(1, 1, 1.0, 1, "p.png", 1, af, fn);
        af.add(new AffineCoeffs(0, 0, 0, 0, 0, 0));
        fn.add(new WeightedVariationRef("swirl", 0.5));
        assertThat(c.affineParams()).hasSize(1);
        assertThat(c.functions()).hasSize(1);
    }
}
