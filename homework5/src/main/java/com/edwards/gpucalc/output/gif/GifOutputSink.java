package com.edwards.gpucalc.output.gif;

import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.ColorPipeline;
import com.edwards.gpucalc.core.Histogram;
import com.edwards.gpucalc.output.EpochSink;
import com.edwards.gpucalc.output.OutputId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@OutputId("gif")
public final class GifOutputSink implements EpochSink {

    static final int PALETTE_SIZE = 256;

    private final ColorPipeline pipeline = ColorPipeline.defaultPipeline();
    private List<BufferedImage> frames = new ArrayList<>();

    @Override
    public void beginEpochs(ResolvedConfig config) throws IOException {
        frames = new ArrayList<>();
        Path outputPath = Path.of(config.outputPath());
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    @Override
    public void writeEpoch(Histogram snapshot, int index, int totalEpochs, ResolvedConfig config) {
        BufferedImage img = pipeline.toImage(snapshot);
        frames.add(img);
        log.info("gif sink: buffered epoch {}/{}", index, totalEpochs);
    }

    @Override
    public void endEpochs(ResolvedConfig config) throws IOException {
        if (frames.isEmpty()) {
            throw new IOException("gif sink: no frames buffered");
        }
        BufferedImage[] arr = frames.toArray(new BufferedImage[0]);
        int width = arr[0].getWidth();
        int height = arr[0].getHeight();

        MedianCutQuantizer quantizer = new MedianCutQuantizer(PALETTE_SIZE);
        MedianCutQuantizer.Result q = quantizer.quantize(arr);

        Path target = Path.of(config.outputPath());
        Gif89aWriter writer = new Gif89aWriter(width, height, q.palette(), config.gifDelayCentiseconds());
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            writer.write(q.indices(), out);
        }
        log.info("wrote GIF: {} ({}x{}, {} frame(s), {} palette entries, delay {}cs)",
                target.toAbsolutePath(), width, height, arr.length, q.palette().length,
                config.gifDelayCentiseconds());

        frames.clear();
    }
}
