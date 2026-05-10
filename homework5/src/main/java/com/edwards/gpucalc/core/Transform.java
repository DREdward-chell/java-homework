package com.edwards.gpucalc.core;

public final class Transform {

    private final AffineCoeffs affine;
    private final Variation[] variations;
    private final double[] weights;
    private final double colorCoord;

    public Transform(AffineCoeffs affine, Variation[] variations, double[] weights, double colorCoord) {
        if (variations.length != weights.length) {
            throw new IllegalArgumentException(
                    "variations.length (" + variations.length + ") != weights.length (" + weights.length + ")");
        }
        if (variations.length == 0) {
            throw new IllegalArgumentException("Transform needs at least one variation");
        }
        if (colorCoord < 0.0 || colorCoord > 1.0) {
            throw new IllegalArgumentException("colorCoord must be in [0,1]; got " + colorCoord);
        }
        this.affine = affine;
        this.variations = variations.clone();
        this.weights = weights.clone();
        this.colorCoord = colorCoord;
    }

    public AffineCoeffs affine() {
        return affine;
    }

    public double colorCoord() {
        return colorCoord;
    }

    public int variationCount() {
        return variations.length;
    }

    public void apply(double x, double y, double[] affineOut, double[] out) {
        affine.apply(x, y, affineOut);
        double ax = affineOut[0];
        double ay = affineOut[1];
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < variations.length; i++) {
            variations[i].apply(ax, ay, out);
            sumX += weights[i] * out[0];
            sumY += weights[i] * out[1];
        }
        out[0] = sumX;
        out[1] = sumY;
    }
}
