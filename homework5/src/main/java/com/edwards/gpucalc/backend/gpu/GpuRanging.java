package com.edwards.gpucalc.backend.gpu;

import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;

import java.util.List;
import java.util.Random;

public final class GpuRanging {

    public static final int RANGING_ITERATIONS = 10_000;
    public static final int WARMUP_ITERATIONS = 20;
    private static final long SEED_MIX = 0x9E3779B97F4A7C15L;

    private GpuRanging() {}

    public static Viewport find(ResolvedConfig config,
                                List<AffineCoeffs> affines,
                                List<WeightedVariationRef> variations) {
        long masterSeed = Double.doubleToLongBits(config.seed()) ^ SEED_MIX;
        Random rng = new Random(masterSeed);
        double x = 0, y = 0;
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double[] scratch = new double[2];
        for (int i = 0; i < RANGING_ITERATIONS + WARMUP_ITERATIONS; i++) {
            AffineCoeffs aff = affines.get(rng.nextInt(affines.size()));
            double ax = aff.a() * x + aff.b() * y + aff.c();
            double ay = aff.d() * x + aff.e() * y + aff.f();
            double sumX = 0, sumY = 0;
            for (WeightedVariationRef ref : variations) {
                applyVariationCpu(ref.name(), ax, ay, scratch);
                sumX += ref.weight() * scratch[0];
                sumY += ref.weight() * scratch[1];
            }
            x = sumX;
            y = sumY;
            if (i < WARMUP_ITERATIONS) continue;
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        if (!Double.isFinite(minX) || minX == maxX || minY == maxY) {
            return new Viewport(-1, 1, -1, 1);
        }
        double padX = (maxX - minX) * 0.02;
        double padY = (maxY - minY) * 0.02;
        return new Viewport(minX - padX, maxX + padX, minY - padY, maxY + padY);
    }

    private static void applyVariationCpu(String id, double x, double y, double[] out) {
        switch (id) {
            case "linear" -> { out[0] = x; out[1] = y; }
            case "sinusoidal" -> { out[0] = Math.sin(x); out[1] = Math.sin(y); }
            case "spherical" -> {
                double r2 = x * x + y * y;
                if (r2 == 0) { out[0] = 0; out[1] = 0; }
                else { out[0] = x / r2; out[1] = y / r2; }
            }
            case "swirl" -> {
                double r2 = x * x + y * y;
                double s = Math.sin(r2), c = Math.cos(r2);
                out[0] = x * s - y * c;
                out[1] = x * c + y * s;
            }
            case "horseshoe" -> {
                double r = Math.sqrt(x * x + y * y);
                if (r == 0) { out[0] = 0; out[1] = 0; }
                else { out[0] = (x - y) * (x + y) / r; out[1] = 2 * x * y / r; }
            }
            default -> throw new IllegalStateException(
                    "variation '" + id + "' not supported by GPU CPU ranging (supported: "
                            + "linear, sinusoidal, spherical, swirl, horseshoe)");
        }
    }

    public record Viewport(double minX, double maxX, double minY, double maxY) {}
}
