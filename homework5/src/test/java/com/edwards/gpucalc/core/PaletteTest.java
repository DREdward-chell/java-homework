package com.edwards.gpucalc.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaletteTest {

    @Test
    void defaultPaletteChannelsAreInUnitInterval() {
        Palette p = Palette.defaultPalette();
        double[] out = new double[3];
        for (int i = 0; i <= 100; i++) {
            double c = i / 100.0;
            p.sample(c, out);
            assertThat(out[0]).isBetween(0.0, 1.0);
            assertThat(out[1]).isBetween(0.0, 1.0);
            assertThat(out[2]).isBetween(0.0, 1.0);
        }
    }

    @Test
    void sampleClampsOutOfRangeCoord() {
        Palette p = Palette.defaultPalette();
        double[] below = new double[3];
        double[] above = new double[3];
        p.sample(-0.5, below);
        p.sample(1.5, above);
        assertThat(below[0]).isEqualTo(p.red(0.0));
        assertThat(above[0]).isEqualTo(p.red(1.0));
    }

    @Test
    void channelAccessorsMatchSample() {
        Palette p = Palette.defaultPalette();
        double[] out = new double[3];
        p.sample(0.5, out);
        assertThat(p.red(0.5)).isEqualTo(out[0]);
        assertThat(p.green(0.5)).isEqualTo(out[1]);
        assertThat(p.blue(0.5)).isEqualTo(out[2]);
    }

    @Test
    void constructorRejectsWrongSize() {
        assertThatThrownBy(() -> new Palette(new double[10], new double[256], new double[256]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customPaletteReturnsConfiguredValues() {
        double[] r = new double[256];
        double[] g = new double[256];
        double[] b = new double[256];
        for (int i = 0; i < 256; i++) {
            r[i] = i / 255.0;
            g[i] = 0.5;
            b[i] = 1.0 - i / 255.0;
        }
        Palette p = new Palette(r, g, b);
        assertThat(p.red(0.0)).isEqualTo(0.0);
        assertThat(p.red(1.0)).isEqualTo(1.0);
        assertThat(p.green(0.42)).isEqualTo(0.5);
        assertThat(p.blue(1.0)).isEqualTo(0.0);
    }
}
