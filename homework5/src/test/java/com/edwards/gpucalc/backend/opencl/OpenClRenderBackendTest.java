package com.edwards.gpucalc.backend.opencl;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.backend.java.JavaRenderBackend;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.ColorPipeline;
import com.edwards.gpucalc.core.Histogram;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GpuCalcApplication.class)
class OpenClRenderBackendTest {

    @Autowired
    Optional<OpenClRenderBackend> openclBackend;
    @Autowired
    JavaRenderBackend javaBackend;
    private final ColorPipeline pipeline = ColorPipeline.defaultPipeline();

    @BeforeAll
    static void skipIfNoOpenCl() {
        Assumptions.assumeTrue(ClAvailable.available(),
                "No OpenCL ICD/device available on this host");
    }

    @Test
    void tinyRenderProducesNonZeroHistogram() {
        assertThat(openclBackend).isPresent();
        OpenClRenderBackend backend = openclBackend.get();
        ResolvedConfig cfg = configFor(64, 64, 200_000);
        RenderResult r = backend.render(cfg);
        assertThat(r.backendId()).isEqualTo("opencl");
        assertThat(r.histogram().totalCount()).isGreaterThan(0L);
    }

    @Test
    void numericalAgreementWithJavaBackend() {
        assertThat(openclBackend).isPresent();
        ResolvedConfig cfg = configFor(64, 64, 1_000_000);
        Histogram javaHist = javaBackend.render(cfg).histogram();
        Histogram clHist = openclBackend.get().render(cfg).histogram();

        BufferedImage javaImg = pipeline.toImage(javaHist);
        BufferedImage clImg = pipeline.toImage(clHist);

        long total = 64L * 64L;
        long within = 0;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int jp = javaImg.getRGB(x, y);
                int cp = clImg.getRGB(x, y);
                int dr = Math.abs(((jp >> 16) & 0xFF) - ((cp >> 16) & 0xFF));
                int dg = Math.abs(((jp >> 8) & 0xFF) - ((cp >> 8) & 0xFF));
                int db = Math.abs((jp & 0xFF) - (cp & 0xFF));
                if (Math.max(dr, Math.max(dg, db)) <= 8) within++;
            }
        }
        double ratio = within / (double) total;
        assertThat(ratio).as("pixels within delta-8 agreement").isGreaterThanOrEqualTo(0.99);
    }

    private static ResolvedConfig configFor(int w, int h, int iter) {
        return new ResolvedConfig(
                w, h, 32.123531, iter, "result.png", 1,
                List.of(new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0),
                        new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5)),
                List.of(new WeightedVariationRef("swirl", 1.0),
                        new WeightedVariationRef("linear", 0.3)));
    }
}
