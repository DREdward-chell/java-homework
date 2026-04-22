package com.edwards.gpucalc.output.gif;

import com.edwards.gpucalc.config.EpochConfig;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.Histogram;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GifOutputSinkTest {

    private final GifOutputSink sink = new GifOutputSink();

    @Test
    void fiveEpochRenderProducesFiveFrameGif(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("anim.gif");
        ResolvedConfig cfg = cfgWith(out.toString(), new EpochConfig(5, 100), 10);
        sink.beginEpochs(cfg);
        for (int i = 1; i <= 5; i++) {
            Histogram h = new Histogram(6, 4);
            for (int k = 0; k < i * 3; k++) {
                h.plot(k % 6, (k * 7) % 4, 1.0, 0.5, 0.2 * i);
            }
            sink.writeEpoch(h, i, 5, cfg);
        }
        sink.endEpochs(cfg);

        assertThat(Files.exists(out)).isTrue();
        assertThat(countFrames(out)).isEqualTo(5);
    }

    @Test
    void singleFrameGifFromWriteDefault(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("solo.gif");
        ResolvedConfig cfg = cfgWith(out.toString(), null, 20);
        Histogram h = new Histogram(3, 3);
        h.plot(1, 1, 1.0, 0.0, 0.0);
        sink.write(new com.edwards.gpucalc.backend.RenderResult(
                h, 1, java.time.Duration.ofMillis(1), "java"), cfg);
        assertThat(countFrames(out)).isEqualTo(1);
    }

    @Test
    void delayOptionIsReflectedInOutput(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("delay.gif");
        int delay = 50;
        ResolvedConfig cfg = cfgWith(out.toString(), new EpochConfig(2, 10), delay);
        sink.beginEpochs(cfg);
        for (int i = 1; i <= 2; i++) {
            Histogram h = new Histogram(2, 2);
            h.plot(i - 1, 0, 1.0, 1.0, 1.0);
            sink.writeEpoch(h, i, 2, cfg);
        }
        sink.endEpochs(cfg);

        byte[] bytes = Files.readAllBytes(out);
        boolean found = containsGraphicControlWithDelay(bytes, delay);
        assertThat(found).isTrue();
    }

    @Test
    void endEpochsWithoutFramesFails(@TempDir Path tmp) throws IOException {
        ResolvedConfig cfg = cfgWith(tmp.resolve("empty.gif").toString(), new EpochConfig(1, 1), 10);
        sink.beginEpochs(cfg);
        assertThatThrownBy(() -> sink.endEpochs(cfg)).isInstanceOf(IOException.class);
    }

    private static boolean containsGraphicControlWithDelay(byte[] bytes, int delay) {
        int lo = delay & 0xFF;
        int hi = (delay >>> 8) & 0xFF;
        for (int i = 0; i < bytes.length - 7; i++) {
            if ((bytes[i] & 0xFF) == 0x21
                    && (bytes[i + 1] & 0xFF) == 0xF9
                    && (bytes[i + 2] & 0xFF) == 4
                    && (bytes[i + 4] & 0xFF) == lo
                    && (bytes[i + 5] & 0xFF) == hi) {
                return true;
            }
        }
        return false;
    }

    private static int countFrames(Path gif) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(gif.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) throw new IOException("no GIF reader");
            ImageReader reader = readers.next();
            reader.setInput(iis);
            return reader.getNumImages(true);
        }
    }

    private static ResolvedConfig cfgWith(String path, EpochConfig epochs, int delay) {
        return new ResolvedConfig(6, 4, 1.0, 1, path, 1,
                List.of(new AffineCoeffs(1, 0, 0, 0, 1, 0)),
                List.of(new WeightedVariationRef("linear", 1.0)),
                epochs, delay);
    }
}
