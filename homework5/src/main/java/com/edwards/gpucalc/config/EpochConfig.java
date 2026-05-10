package com.edwards.gpucalc.config;

public record EpochConfig(int epochCount, int epochStride) {
    public EpochConfig {
        if (epochCount < 1) {
            throw new IllegalArgumentException("epochCount must be >= 1 (got " + epochCount + ")");
        }
        if (epochStride < 1) {
            throw new IllegalArgumentException("epochStride must be >= 1 (got " + epochStride + ")");
        }
    }
}
