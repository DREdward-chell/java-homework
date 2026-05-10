package com.edwards.gpucalc.output.png;

import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.Histogram;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PngOutputSinkTest {

    private final PngOutputSink sink = new PngOutputSink();

    @Test
    void writesReadablePngWithCorrectDimensions(@TempDir Path tmp) throws IOException {
        Histogram hist = new Histogram(8, 6);
        hist.plot(3, 2, 1.0, 0.5, 0.25);
        RenderResult result = new RenderResult(hist, 1, Duration.ofMillis(1), "java");
        Path out = tmp.resolve("out.png");
        ResolvedConfig cfg = cfgWith(out.toString());
        sink.write(result, cfg);

        assertThat(Files.exists(out)).isTrue();
        BufferedImage img = ImageIO.read(out.toFile());
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(8);
        assertThat(img.getHeight()).isEqualTo(6);
    }

    @Test
    void writesBrightPixelForDensePoint(@TempDir Path tmp) throws IOException {
        Histogram hist = new Histogram(4, 4);
        for (int i = 0; i < 50; i++) hist.plot(1, 1, 1.0, 1.0, 1.0);
        RenderResult result = new RenderResult(hist, 50, Duration.ofMillis(1), "java");
        Path out = tmp.resolve("bright.png");
        sink.write(result, cfgWith(out.toString()));

        BufferedImage img = ImageIO.read(out.toFile());
        int pixel = img.getRGB(1, 1) & 0xFFFFFF;
        assertThat(pixel).isGreaterThan(0);
    }

    @Test
    void createsMissingParentDirectory(@TempDir Path tmp) throws IOException {
        Histogram hist = new Histogram(2, 2);
        hist.plot(0, 0, 0.5, 0.5, 0.5);
        RenderResult result = new RenderResult(hist, 1, Duration.ofMillis(1), "java");
        Path out = tmp.resolve("nested").resolve("deeper").resolve("file.png");
        sink.write(result, cfgWith(out.toString()));
        assertThat(Files.exists(out)).isTrue();
    }

    @Test
    void unwritablePathThrowsIOException() {
        Histogram hist = new Histogram(2, 2);
        RenderResult result = new RenderResult(hist, 0, Duration.ofMillis(0), "java");
        ResolvedConfig cfg = cfgWith("/no-such-root/deeply/nested/that/cannot/be/created/forbidden.png");
        assertThatThrownBy(() -> sink.write(result, cfg))
                .isInstanceOf(IOException.class);
    }

    private static ResolvedConfig cfgWith(String path) {
        return new ResolvedConfig(8, 6, 1.0, 1, path, 1,
                List.of(new AffineCoeffs(1, 0, 0, 0, 1, 0)),
                List.of(new WeightedVariationRef("linear", 1.0)));
    }
}
