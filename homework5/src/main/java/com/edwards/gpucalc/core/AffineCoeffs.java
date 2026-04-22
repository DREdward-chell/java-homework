package com.edwards.gpucalc.core;

public record AffineCoeffs(double a, double b, double c, double d, double e, double f) {

    public void apply(double x, double y, double[] out) {
        out[0] = a * x + b * y + c;
        out[1] = d * x + e * y + f;
    }

    public static AffineCoeffs identity() {
        return new AffineCoeffs(1, 0, 0, 0, 1, 0);
    }
}
