package com.edwards.gpucalc.output.epoch;

import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.ColorPipeline;
import com.edwards.gpucalc.core.Histogram;
import com.edwards.gpucalc.output.EpochSink;
import com.edwards.gpucalc.output.OutputId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Component
@OutputId("epochs")
public final class EpochPngOutputSink implements EpochSink {

    private final ColorPipeline pipeline = ColorPipeline.defaultPipeline();

    @Override
    public void beginEpochs(ResolvedConfig config) throws IOException {
        Path outputPath = Path.of(config.outputPath());
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    @Override
    public void writeEpoch(Histogram snapshot, int index, int totalEpochs, ResolvedConfig config)
            throws IOException {
        int digits = Math.max(4, Integer.toString(totalEpochs).length());
        Path target = epochPath(config.outputPath(), index, digits);
        BufferedImage img = pipeline.toImage(snapshot);
        boolean ok = ImageIO.write(img, "png", target.toFile());
        if (!ok) {
            throw new IOException("ImageIO could not write PNG for path: " + target);
        }
        log.info("wrote epoch {}/{}: {}", index, totalEpochs, target.toAbsolutePath());
    }

    @Override
    public void endEpochs(ResolvedConfig config) {
        log.info("epoch sink: done");
    }

    static Path epochPath(String configured, int index, int digits) {
        int dot = configured.lastIndexOf('.');
        int sep = Math.max(configured.lastIndexOf('/'), configured.lastIndexOf('\\'));
        String base;
        String ext;
        if (dot > sep) {
            base = configured.substring(0, dot);
            ext = configured.substring(dot);
        } else {
            base = configured;
            ext = ".png";
        }
        String format = "%0" + digits + "d";
        return Path.of(base + "_" + String.format(Locale.ROOT, format, index) + ext);
    }
}
