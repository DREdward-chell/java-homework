package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;

import java.util.List;

public final class Defaults {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final double SEED = 5.0;
    public static final int ITERATION_COUNT = 2500;
    public static final String OUTPUT_PATH = "result.png";
    public static final int THREADS = 1;

    public static final List<AffineCoeffs> AFFINE_PARAMS = List.of(
            new AffineCoeffs(0.5, 0.0, 0.0, 0.0, 0.5, 0.0),
            new AffineCoeffs(0.5, 0.0, 0.5, 0.0, 0.5, 0.5)
    );

    public static final List<WeightedVariationRef> FUNCTIONS = List.of();

    private Defaults() {
    }
}
