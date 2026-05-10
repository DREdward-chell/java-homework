package com.edwards.gpucalc.backend.opengl;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.backend.java.JavaRenderBackend;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.ColorPipeline;
import com.edwards.gpucalc.core.Histogram;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnOs(OS.MAC)
@SpringBootTest(classes = GpuCalcApplication.class)
class OpenGlComputeRenderBackendTest {

    @Autowired
    Optional<OpenGlComputeRenderBackend> openglBackend;
    @Autowired
    JavaRenderBackend javaBackend;
    private final ColorPipeline pipeline = ColorPipeline.defaultPipeline();

    @BeforeAll
    static void ensureContextOrSkip() {
        try {
            GlContext c = GlContext.createHeadless();
            c.close();
        } catch (IllegalStateException | UnsatisfiedLinkError e) {
            org.junit.jupiter.api.Assumptions.abort("no GL 4.3 headless context: " + e.getMessage());
        }
    }

    @Test
    void tinyRenderProducesNonZeroHistogram() {
        assertThat(openglBackend).isPresent();
        OpenGlComputeRenderBackend backend = openglBackend.get();
        ResolvedConfig cfg = configFor(64, 64, 200_000);
        RenderResult r = backend.render(cfg);
        assertThat(r.backendId()).isEqualTo("opengl-compute");
        assertThat(r.histogram().totalCount()).isGreaterThan(0L);
    }

    @Test
    void numericalAgreementWithJavaBackend() {
        assertThat(openglBackend).isPresent();
        ResolvedConfig cfg = configFor(64, 64, 1_000_000);
        Histogram javaHist = javaBackend.render(cfg).histogram();
        Histogram glHist = openglBackend.get().render(cfg).histogram();

        BufferedImage javaImg = pipeline.toImage(javaHist);
        BufferedImage glImg = pipeline.toImage(glHist);

        long total = 64L * 64L;
        long within = 0;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int jp = javaImg.getRGB(x, y);
                int gp = glImg.getRGB(x, y);
                int dr = Math.abs(((jp >> 16) & 0xFF) - ((gp >> 16) & 0xFF));
                int dg = Math.abs(((jp >> 8) & 0xFF) - ((gp >> 8) & 0xFF));
                int db = Math.abs((jp & 0xFF) - (gp & 0xFF));
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
