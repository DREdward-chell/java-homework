package com.edwards.gpucalc.core;

public final class Histogram {

    private final int width;
    private final int height;
    private final long[] counts;
    private final double[] rSum;
    private final double[] gSum;
    private final double[] bSum;

    public Histogram(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0; got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        int n = width * height;
        this.counts = new long[n];
        this.rSum = new double[n];
        this.gSum = new double[n];
        this.bSum = new double[n];
    }

    public int width() { return width; }
    public int height() { return height; }

    public void plot(int px, int py, double r, double g, double b) {
        int idx = py * width + px;
        counts[idx]++;
        rSum[idx] += r;
        gSum[idx] += g;
        bSum[idx] += b;
    }

    public boolean inBounds(int px, int py) {
        return px >= 0 && px < width && py >= 0 && py < height;
    }

    public long count(int px, int py) {
        return counts[py * width + px];
    }

    public double rSum(int px, int py) { return rSum[py * width + px]; }
    public double gSum(int px, int py) { return gSum[py * width + px]; }
    public double bSum(int px, int py) { return bSum[py * width + px]; }

    public long[] counts() { return counts; }
    public double[] redAccumulator() { return rSum; }
    public double[] greenAccumulator() { return gSum; }
    public double[] blueAccumulator() { return bSum; }

    public long maxCount() {
        long m = 0;
        for (long v : counts) if (v > m) m = v;
        return m;
    }

    public long totalCount() {
        long t = 0;
        for (long v : counts) t += v;
        return t;
    }

    public void mergeFrom(Histogram other) {
        if (other.width != width || other.height != height) {
            throw new IllegalArgumentException("histogram shape mismatch: "
                    + width + "x" + height + " vs " + other.width + "x" + other.height);
        }
        for (int i = 0; i < counts.length; i++) {
            counts[i] += other.counts[i];
            rSum[i]   += other.rSum[i];
            gSum[i]   += other.gSum[i];
            bSum[i]   += other.bSum[i];
        }
    }
}
