package com.edwards.gpucalc.backend.java;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@SpringBootTest(classes = GpuCalcApplication.class)
class JavaRenderBackendStressTest {

    @Autowired
    JavaRenderBackend backend;

    @Test
    void fourKEightThreadsCompletesWithoutOom() {
        ResolvedConfig cfg = new ResolvedConfig(
                4096, 4096, 7.0, 1_000_000, "stress.png", 8,
                List.of(new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0),
                        new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5)),
                List.of(new WeightedVariationRef("linear", 1.0)));
        RenderResult r = backend.render(cfg);
        assertThat(r.histogram().totalCount()).isGreaterThan(0L);
    }
}
