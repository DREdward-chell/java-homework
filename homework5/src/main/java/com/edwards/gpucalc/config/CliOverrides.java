package com.edwards.gpucalc.config;

import org.jspecify.annotations.Nullable;

public record CliOverrides(
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Double seed,
        @Nullable Integer iterationCount,
        @Nullable String outputPath,
        @Nullable Integer threads,
        @Nullable String affineParams,
        @Nullable String functions,
        @Nullable Integer epochCount,
        @Nullable Integer epochStride,
        @Nullable Integer gifDelayCentiseconds,
        @Nullable String clPlatform,
        @Nullable String clDevice
) {
    public CliOverrides(@Nullable Integer width, @Nullable Integer height,
                        @Nullable Double seed, @Nullable Integer iterationCount,
                        @Nullable String outputPath, @Nullable Integer threads,
                        @Nullable String affineParams, @Nullable String functions) {
        this(width, height, seed, iterationCount, outputPath, threads, affineParams, functions,
                null, null, null, null, null);
    }

    public CliOverrides(@Nullable Integer width, @Nullable Integer height,
                        @Nullable Double seed, @Nullable Integer iterationCount,
                        @Nullable String outputPath, @Nullable Integer threads,
                        @Nullable String affineParams, @Nullable String functions,
                        @Nullable Integer epochCount, @Nullable Integer epochStride,
                        @Nullable Integer gifDelayCentiseconds) {
        this(width, height, seed, iterationCount, outputPath, threads, affineParams, functions,
                epochCount, epochStride, gifDelayCentiseconds, null, null);
    }

    public static CliOverrides empty() {
        return new CliOverrides(null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
