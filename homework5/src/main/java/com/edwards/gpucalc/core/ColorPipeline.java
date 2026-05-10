package com.edwards.gpucalc.core;

import java.awt.image.BufferedImage;

public final class ColorPipeline {

    private final double gamma;
    private final double vibrancy;

    public ColorPipeline(double gamma, double vibrancy) {
        if (gamma <= 0.0) {
            throw new IllegalArgumentException("gamma must be > 0; got " + gamma);
        }
        if (vibrancy < 0.0 || vibrancy > 1.0) {
            throw new IllegalArgumentException("vibrancy must be in [0,1]; got " + vibrancy);
        }
        this.gamma = gamma;
        this.vibrancy = vibrancy;
    }

    public static ColorPipeline defaultPipeline() {
        return new ColorPipeline(2.2, 1.0);
    }

    public BufferedImage toImage(Histogram hist) {
        int w = hist.width();
        int h = hist.height();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long maxCount = hist.maxCount();
        if (maxCount == 0) {
            return img;
        }
        double logMax = Math.log1p(maxCount);
        double invGamma = 1.0 / gamma;
        long[] counts = hist.counts();
        double[] rSum = hist.redAccumulator();
        double[] gSum = hist.greenAccumulator();
        double[] bSum = hist.blueAccumulator();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                long n = counts[idx];
                if (n == 0) continue;

                double alpha = Math.log1p(n) / logMax;
                double invN = 1.0 / n;
                double rLin = clamp01(rSum[idx] * invN);
                double gLin = clamp01(gSum[idx] * invN);
                double bLin = clamp01(bSum[idx] * invN);

                double alphaGamma = Math.pow(alpha, invGamma);
                double rChGamma = Math.pow(rLin, invGamma);
                double gChGamma = Math.pow(gLin, invGamma);
                double bChGamma = Math.pow(bLin, invGamma);

                double r = (vibrancy * alphaGamma * rChGamma) + ((1.0 - vibrancy) * alphaGamma * rLin);
                double g = (vibrancy * alphaGamma * gChGamma) + ((1.0 - vibrancy) * alphaGamma * gLin);
                double b = (vibrancy * alphaGamma * bChGamma) + ((1.0 - vibrancy) * alphaGamma * bLin);

                int ri = toByte(r);
                int gi = toByte(g);
                int bi = toByte(b);
                img.setRGB(x, y, (ri << 16) | (gi << 8) | bi);
            }
        }
        return img;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static int toByte(double v) {
        if (v <= 0.0) return 0;
        if (v >= 1.0) return 255;
        return (int) Math.round(v * 255.0);
    }
}
