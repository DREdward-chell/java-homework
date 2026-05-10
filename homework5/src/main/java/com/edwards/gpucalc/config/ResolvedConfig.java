package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ResolvedConfig(
        int width,
        int height,
        double seed,
        int iterationCount,
        String outputPath,
        int threads,
        List<AffineCoeffs> affineParams,
        List<WeightedVariationRef> functions,
        @Nullable EpochConfig epochs,
        int gifDelayCentiseconds,
        @Nullable String clPlatform,
        @Nullable String clDevice
) {
    public static final int DEFAULT_GIF_DELAY_CS = 10;

    public ResolvedConfig {
        affineParams = List.copyOf(affineParams);
        functions = List.copyOf(functions);
    }

    public ResolvedConfig(int width, int height, double seed, int iterationCount,
                          String outputPath, int threads,
                          List<AffineCoeffs> affineParams,
                          List<WeightedVariationRef> functions) {
        this(width, height, seed, iterationCount, outputPath, threads, affineParams, functions,
                null, DEFAULT_GIF_DELAY_CS, null, null);
    }

    public ResolvedConfig(int width, int height, double seed, int iterationCount,
                          String outputPath, int threads,
                          List<AffineCoeffs> affineParams,
                          List<WeightedVariationRef> functions,
                          @Nullable EpochConfig epochs,
                          int gifDelayCentiseconds) {
        this(width, height, seed, iterationCount, outputPath, threads, affineParams, functions,
                epochs, gifDelayCentiseconds, null, null);
    }
}
