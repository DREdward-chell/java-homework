package com.edwards.gpucalc.core;

public final class Palette {

    public static final int SIZE = 256;

    private final double[] r = new double[SIZE];
    private final double[] g = new double[SIZE];
    private final double[] b = new double[SIZE];

    public Palette(double[] r, double[] g, double[] b) {
        if (r.length != SIZE || g.length != SIZE || b.length != SIZE) {
            throw new IllegalArgumentException("palette channels must each have " + SIZE + " entries");
        }
        System.arraycopy(r, 0, this.r, 0, SIZE);
        System.arraycopy(g, 0, this.g, 0, SIZE);
        System.arraycopy(b, 0, this.b, 0, SIZE);
    }

    public static Palette defaultPalette() {
        double[] r = new double[SIZE];
        double[] g = new double[SIZE];
        double[] b = new double[SIZE];
        for (int i = 0; i < SIZE; i++) {
            double t = i / (double) (SIZE - 1);
            double phase = t * 2.0 * Math.PI;
            r[i] = 0.5 + 0.5 * Math.sin(phase);
            g[i] = 0.5 + 0.5 * Math.sin(phase + 2.0 * Math.PI / 3.0);
            b[i] = 0.5 + 0.5 * Math.sin(phase + 4.0 * Math.PI / 3.0);
        }
        return new Palette(r, g, b);
    }

    public double red(double c)   { return r[index(c)]; }
    public double green(double c) { return g[index(c)]; }
    public double blue(double c)  { return b[index(c)]; }

    public void sample(double c, double[] out) {
        int i = index(c);
        out[0] = r[i];
        out[1] = g[i];
        out[2] = b[i];
    }

    private static int index(double c) {
        if (c <= 0.0) return 0;
        if (c >= 1.0) return SIZE - 1;
        return (int) (c * (SIZE - 1));
    }
}
