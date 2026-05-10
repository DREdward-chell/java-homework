package com.edwards.gpucalc.output.png;

import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.ColorPipeline;
import com.edwards.gpucalc.output.OutputId;
import com.edwards.gpucalc.output.OutputSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@OutputId("png")
public final class PngOutputSink implements OutputSink {

    private final ColorPipeline pipeline = ColorPipeline.defaultPipeline();

    @Override
    public void write(RenderResult result, ResolvedConfig config) throws IOException {
        BufferedImage img = pipeline.toImage(result.histogram());
        Path outputPath = Path.of(config.outputPath());
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        boolean ok = ImageIO.write(img, "png", outputPath.toFile());
        if (!ok) {
            throw new IOException("ImageIO could not write PNG for path: " + outputPath);
        }
        log.info("wrote PNG: {} ({}x{})", outputPath.toAbsolutePath(), img.getWidth(), img.getHeight());
    }
}
