package com.edwards.gpucalc.core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColorPipelineTest {

    @Test
    void emptyHistogramRendersAllBlack() {
        Histogram h = new Histogram(4, 4);
        BufferedImage img = ColorPipeline.defaultPipeline().toImage(h);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                assertThat(img.getRGB(x, y) & 0xFFFFFF).isZero();
            }
        }
    }

    @Test
    void maxDensityPixelProducesFullBrightness() {
        Histogram h = new Histogram(2, 2);
        h.plot(0, 0, 1.0, 1.0, 1.0);
        BufferedImage img = ColorPipeline.defaultPipeline().toImage(h);
        int rgb = img.getRGB(0, 0) & 0xFFFFFF;
        assertThat(rgb).isEqualTo(0xFFFFFF);
    }

    @Test
    void unplottedPixelStaysBlack() {
        Histogram h = new Histogram(2, 2);
        h.plot(0, 0, 1.0, 0.5, 0.0);
        BufferedImage img = ColorPipeline.defaultPipeline().toImage(h);
        assertThat(img.getRGB(1, 1) & 0xFFFFFF).isZero();
    }

    @Test
    void outputImageHasCorrectDimensions() {
        Histogram h = new Histogram(7, 5);
        BufferedImage img = ColorPipeline.defaultPipeline().toImage(h);
        assertThat(img.getWidth()).isEqualTo(7);
        assertThat(img.getHeight()).isEqualTo(5);
        assertThat(img.getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void higherDensityIsBrighterThanLowerDensity() {
        Histogram h = new Histogram(2, 1);
        h.plot(0, 0, 1.0, 1.0, 1.0);
        for (int i = 0; i < 100; i++) {
            h.plot(1, 0, 1.0, 1.0, 1.0);
        }
        BufferedImage img = ColorPipeline.defaultPipeline().toImage(h);
        int dim = img.getRGB(0, 0) & 0xFF;
        int bright = img.getRGB(1, 0) & 0xFF;
        assertThat(bright).isGreaterThan(dim);
    }

    @Test
    void constructorRejectsNonPositiveGamma() {
        assertThatThrownBy(() -> new ColorPipeline(0, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsOutOfRangeVibrancy() {
        assertThatThrownBy(() -> new ColorPipeline(2.2, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ColorPipeline(2.2, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
