package com.edwards.gpucalc.backend.java;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.EpochConfig;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.Histogram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class JavaRenderBackendEpochTest {

    @Autowired
    JavaRenderBackend backend;

    @Test
    void callbackCalledOncePerEpoch() {
        ResolvedConfig cfg = configFor(10_000, 1, new EpochConfig(5, 1_000));
        AtomicInteger count = new AtomicInteger();
        RenderResult r = backend.renderEpochs(cfg, (snap, idx, total) -> {
            count.incrementAndGet();
            assertThat(idx).isBetween(1, 5);
            assertThat(total).isEqualTo(5);
            assertThat(snap.width()).isEqualTo(32);
        });
        assertThat(count.get()).isEqualTo(5);
        assertThat(r.histogram().totalCount()).isGreaterThan(0L);
    }

    @Test
    void snapshotsAreMonotonicallyNonDecreasing() {
        ResolvedConfig cfg = configFor(8_000, 1, new EpochConfig(4, 1_500));
        List<Long> totals = new ArrayList<>();
        backend.renderEpochs(cfg, (snap, idx, total) -> totals.add(snap.totalCount()));
        for (int i = 1; i < totals.size(); i++) {
            assertThat(totals.get(i)).isGreaterThanOrEqualTo(totals.get(i - 1));
        }
    }

    @Test
    void finalSnapshotEqualsRenderResultHistogram() {
        ResolvedConfig cfg = configFor(8_000, 1, new EpochConfig(4, 1_500));
        long[] lastTotal = new long[1];
        RenderResult r = backend.renderEpochs(cfg, (snap, idx, total) -> {
            if (idx == total) lastTotal[0] = snap.totalCount();
        });
        assertThat(r.histogram().totalCount()).isEqualTo(lastTotal[0]);
    }

    @Test
    void epochsIndependentOfThreadCountForCallbackArity() {
        ResolvedConfig cfg = configFor(16_000, 4, new EpochConfig(8, 1_500));
        AtomicInteger count = new AtomicInteger();
        backend.renderEpochs(cfg, (snap, idx, total) -> count.incrementAndGet());
        assertThat(count.get()).isEqualTo(8);
    }

    @Test
    void productBeyondIterationCountFails() {
        ResolvedConfig cfg = configFor(100, 1, new EpochConfig(10, 50));
        assertThatThrownBy(() -> backend.renderEpochs(cfg, (s, i, t) -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void renderEpochsWithoutEpochConfigRefuses() {
        ResolvedConfig cfg = configFor(1_000, 1, null);
        assertThatThrownBy(() -> backend.renderEpochs(cfg, (s, i, t) -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tailAbsorbedByFinalEpoch() {
        ResolvedConfig cfg = configFor(10_007, 1, new EpochConfig(5, 1_000));
        long[] totals = new long[5];
        RenderResult r = backend.renderEpochs(cfg, (snap, idx, total) -> totals[idx - 1] = snap.totalCount());
        assertThat(totals[4]).isEqualTo(r.histogram().totalCount());
    }

    @Test
    void snapshotsAreIndependentOfBackendBuffers() {
        ResolvedConfig cfg = configFor(5_000, 1, new EpochConfig(3, 1_000));
        List<Histogram> kept = new ArrayList<>();
        backend.renderEpochs(cfg, (snap, idx, total) -> kept.add(snap));
        long c1 = kept.get(0).totalCount();
        long c2 = kept.get(1).totalCount();
        long c3 = kept.get(2).totalCount();
        assertThat(c1).isLessThanOrEqualTo(c2);
        assertThat(c2).isLessThanOrEqualTo(c3);
    }

    private static ResolvedConfig configFor(int iter, int threads, EpochConfig epochs) {
        return new ResolvedConfig(
                32, 32, 32.123531, iter, "result.png", threads,
                List.of(new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0),
                        new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5)),
                List.of(new WeightedVariationRef("swirl", 1.0),
                        new WeightedVariationRef("linear", 0.3)),
                epochs, ResolvedConfig.DEFAULT_GIF_DELAY_CS);
    }
}
