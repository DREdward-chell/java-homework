package com.edwards.gpucalc.output.gif;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedianCutQuantizerTest {

    @Test
    void promotesUniqueColorsIfBelowPaletteSize() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFF0000);
        img.setRGB(1, 0, 0x00FF00);
        img.setRGB(0, 1, 0x0000FF);
        img.setRGB(1, 1, 0xFFFFFF);

        MedianCutQuantizer.Result r = new MedianCutQuantizer(16).quantize(new BufferedImage[]{img});
        assertThat(r.palette().length).isEqualTo(4);
        assertThat(r.indices().length).isEqualTo(1);
        assertThat(r.indices()[0].length).isEqualTo(4);
    }

    @Test
    void reducesManyColorsToRequestedPaletteSize() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, ((x * 16) << 16) | ((y * 16) << 8));
            }
        }

        MedianCutQuantizer.Result r = new MedianCutQuantizer(8).quantize(new BufferedImage[]{img});
        assertThat(r.palette().length).isLessThanOrEqualTo(8);
        assertThat(r.indices()[0].length).isEqualTo(16 * 16);
        for (byte b : r.indices()[0]) {
            assertThat(b & 0xFF).isLessThan(r.palette().length);
        }
    }

    @Test
    void nearestIndexPointsAtClosePaletteEntry() {
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0x000000);
        img.setRGB(1, 0, 0xFFFFFF);

        MedianCutQuantizer.Result r = new MedianCutQuantizer(2).quantize(new BufferedImage[]{img});
        int ix0 = r.indices()[0][0] & 0xFF;
        int ix1 = r.indices()[0][1] & 0xFF;
        assertThat(ix0).isNotEqualTo(ix1);

        byte[] black = r.palette()[ix0];
        byte[] white = r.palette()[ix1];
        assertThat(black[0] & 0xFF).isLessThan(50);
        assertThat(white[0] & 0xFF).isGreaterThan(200);
    }

    @Test
    void sharedPaletteAcrossMultipleFrames() {
        BufferedImage a = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        a.setRGB(0, 0, 0xFF0000);
        BufferedImage b = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        b.setRGB(0, 0, 0x00FF00);

        MedianCutQuantizer.Result r = new MedianCutQuantizer(4).quantize(new BufferedImage[]{a, b});
        assertThat(r.indices().length).isEqualTo(2);
        assertThat(r.palette().length).isEqualTo(2);
    }

    @Test
    void rejectsPaletteBelowTwo() {
        assertThatThrownBy(() -> new MedianCutQuantizer(1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPaletteAboveMax() {
        assertThatThrownBy(() -> new MedianCutQuantizer(257))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
