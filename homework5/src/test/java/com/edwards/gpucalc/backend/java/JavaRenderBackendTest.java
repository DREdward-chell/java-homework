package com.edwards.gpucalc.backend.java;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.RenderBackend;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.config.WeightedVariationRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GpuCalcApplication.class)
class JavaRenderBackendTest {

    @Autowired
    JavaRenderBackend backend;

    @Test
    void deterministicRenderFillsHistogram() {
        ResolvedConfig cfg = configFor(50_000, 1);
        RenderResult result = backend.render(cfg);
        assertThat(result.histogram().totalCount()).isGreaterThan(1_000L);
        assertThat(result.iterations()).isEqualTo(50_000L);
        assertThat(result.backendId()).isEqualTo("java");
    }

    @Test
    void sameSeedProducesSameHistogramHashSingleThreaded() {
        ResolvedConfig cfg = configFor(20_000, 1);
        RenderBackend b = backend;
        long hashA = histogramHash(b.render(cfg));
        long hashB = histogramHash(b.render(cfg));
        assertThat(hashA).isEqualTo(hashB);
    }

    @Test
    void sameSeedProducesSameHistogramHashMultiThreaded() {
        ResolvedConfig cfg = configFor(40_000, 4);
        long hashA = histogramHash(backend.render(cfg));
        long hashB = histogramHash(backend.render(cfg));
        assertThat(hashA).isEqualTo(hashB);
    }

    @Test
    void multiThreadedTotalMatchesSingleThreadedTotal() {
        ResolvedConfig cfg1 = configFor(80_000, 1);
        ResolvedConfig cfg4 = configFor(80_000, 4);
        RenderResult r1 = backend.render(cfg1);
        RenderResult r4 = backend.render(cfg4);
        assertThat(r4.histogram().totalCount()).isEqualTo(r1.histogram().totalCount());
    }

    @Test
    void threadCountChangesImageContent() {
        ResolvedConfig cfg1 = configFor(60_000, 1);
        ResolvedConfig cfg4 = configFor(60_000, 4);
        long hash1 = histogramHash(backend.render(cfg1));
        long hash4 = histogramHash(backend.render(cfg4));
        assertThat(hash1).isNotEqualTo(hash4);
    }

    @Test
    void smallIterationCountStillProducesValidHistogram() {
        ResolvedConfig cfg = configFor(100, 1);
        RenderResult result = backend.render(cfg);
        assertThat(result.histogram().width()).isEqualTo(64);
        assertThat(result.histogram().height()).isEqualTo(64);
    }

    @Test
    void remainderGoesToThreadZero() {
        ResolvedConfig cfg = configFor(10_007, 4);
        RenderResult result = backend.render(cfg);
        assertThat(result.iterations()).isEqualTo(10_007L);
        assertThat(result.histogram().totalCount()).isGreaterThan(0L);
    }

    private static ResolvedConfig configFor(int iter, int threads) {
        return new ResolvedConfig(
                64, 64, 32.123531, iter, "result.png", threads,
                List.of(new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0),
                        new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5)),
                List.of(new WeightedVariationRef("swirl", 1.0),
                        new WeightedVariationRef("linear", 0.3)));
    }

    private static long histogramHash(RenderResult r) {
        long h = 1L;
        for (long v : r.histogram().counts()) {
            h = h * 31 + v;
        }
        return h;
    }
}
