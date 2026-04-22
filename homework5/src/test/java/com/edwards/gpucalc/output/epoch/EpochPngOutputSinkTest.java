package com.edwards.gpucalc.output.epoch;

import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.EpochConfig;
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

class EpochPngOutputSinkTest {

    private final EpochPngOutputSink sink = new EpochPngOutputSink();

    @Test
    void writesNumberedFileForEachEpoch(@TempDir Path tmp) throws IOException {
        ResolvedConfig cfg = cfgWith(tmp.resolve("out.png").toString(), new EpochConfig(3, 100));
        sink.beginEpochs(cfg);
        for (int i = 1; i <= 3; i++) {
            Histogram h = new Histogram(4, 4);
            h.plot(i % 4, i % 4, 1.0, 1.0, 1.0);
            sink.writeEpoch(h, i, 3, cfg);
        }
        sink.endEpochs(cfg);

        assertThat(Files.exists(tmp.resolve("out_0001.png"))).isTrue();
        assertThat(Files.exists(tmp.resolve("out_0002.png"))).isTrue();
        assertThat(Files.exists(tmp.resolve("out_0003.png"))).isTrue();
    }

    @Test
    void writesUsingExtensionlessBasePath(@TempDir Path tmp) throws IOException {
        ResolvedConfig cfg = cfgWith(tmp.resolve("noext").toString(), new EpochConfig(2, 50));
        sink.beginEpochs(cfg);
        sink.writeEpoch(new Histogram(2, 2), 1, 2, cfg);
        sink.writeEpoch(new Histogram(2, 2), 2, 2, cfg);
        sink.endEpochs(cfg);

        assertThat(Files.exists(tmp.resolve("noext_0001.png"))).isTrue();
        assertThat(Files.exists(tmp.resolve("noext_0002.png"))).isTrue();
    }

    @Test
    void digitsWidensForLargeTotals(@TempDir Path tmp) throws IOException {
        ResolvedConfig cfg = cfgWith(tmp.resolve("big.png").toString(), new EpochConfig(12345, 1));
        sink.beginEpochs(cfg);
        sink.writeEpoch(new Histogram(2, 2), 42, 12345, cfg);
        sink.endEpochs(cfg);

        assertThat(Files.exists(tmp.resolve("big_00042.png"))).isTrue();
    }

    @Test
    void defaultWriteEmitsSingleFrameThroughEpochTrio(@TempDir Path tmp) throws IOException {
        Histogram h = new Histogram(3, 3);
        h.plot(1, 1, 0.5, 0.5, 0.5);
        RenderResult r = new RenderResult(h, 1, Duration.ofMillis(1), "java");
        ResolvedConfig cfg = cfgWith(tmp.resolve("single.png").toString(), null);
        sink.write(r, cfg);

        Path out = tmp.resolve("single_0001.png");
        assertThat(Files.exists(out)).isTrue();
        BufferedImage img = ImageIO.read(out.toFile());
        assertThat(img.getWidth()).isEqualTo(3);
    }

    @Test
    void pathBuilderStripsPngExtension() {
        Path p = EpochPngOutputSink.epochPath("/tmp/result.png", 7, 4);
        assertThat(p.toString()).isEqualTo("/tmp/result_0007.png");
    }

    @Test
    void pathBuilderAppendsPngWhenBaseHasNoExtension() {
        Path p = EpochPngOutputSink.epochPath("/tmp/frame", 1, 4);
        assertThat(p.toString()).isEqualTo("/tmp/frame_0001.png");
    }

    private static ResolvedConfig cfgWith(String path, EpochConfig epochs) {
        return new ResolvedConfig(4, 4, 1.0, 1, path, 1,
                List.of(new AffineCoeffs(1, 0, 0, 0, 1, 0)),
                List.of(new WeightedVariationRef("linear", 1.0)),
                epochs, ResolvedConfig.DEFAULT_GIF_DELAY_CS);
    }
}
