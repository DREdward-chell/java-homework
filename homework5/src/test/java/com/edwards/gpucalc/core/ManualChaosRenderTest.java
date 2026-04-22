package com.edwards.gpucalc.core;

import com.edwards.gpucalc.core.variations.LinearVariation;
import com.edwards.gpucalc.core.variations.SwirlVariation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

@Tag("manual-test")
class ManualChaosRenderTest {

    private static final int W = 800;
    private static final int H = 600;
    private static final int ITERATIONS = 1_000_000;
    private static final int WARMUP = 20;

    @Test
    void linearSierpinskiLikeFlame() throws Exception {
        Transform[] transforms = {
                transform(new AffineCoeffs(0.5, 0, 0,   0, 0.5, 0),   new LinearVariation(), 0.05),
                transform(new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0),   new LinearVariation(), 0.5),
                transform(new AffineCoeffs(0.5, 0, 0,   0, 0.5, 0.5), new LinearVariation(), 0.95),
        };
        renderAndWrite(transforms, "phase2-linear.png");
    }

    @Test
    void swirlFlame() throws Exception {
        Transform[] transforms = {
                transform(new AffineCoeffs(0.5, 0, 0,   0, 0.5, 0), new SwirlVariation(), 0.1),
                transform(new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5), new SwirlVariation(), 0.9),
        };
        renderAndWrite(transforms, "phase2-swirl.png");
    }

    private static Transform transform(AffineCoeffs affine, Variation v, double c) {
        return new Transform(affine, new Variation[]{v}, new double[]{1.0}, c);
    }

    private void renderAndWrite(Transform[] transforms, String filename) throws Exception {
        Histogram hist = new Histogram(W, H);
        Palette palette = Palette.defaultPalette();
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);

        double x = 0.0, y = 0.0, c = 0.5;
        double[] scratch = new double[2];
        double[] out = new double[2];
        double[] rgb = new double[3];

        for (int i = 0; i < WARMUP + ITERATIONS; i++) {
            Transform t = transforms[rng.nextInt(transforms.length)];
            t.apply(x, y, scratch, out);
            x = out[0];
            y = out[1];
            c = (c + t.colorCoord()) * 0.5;
            if (i < WARMUP) continue;
            int px = (int) ((x + 1.0) * 0.5 * (W - 1));
            int py = (int) ((1.0 - (y + 1.0) * 0.5) * (H - 1));
            if (!hist.inBounds(px, py)) continue;
            palette.sample(c, rgb);
            hist.plot(px, py, rgb[0], rgb[1], rgb[2]);
        }

        BufferedImage img = ColorPipeline.defaultPipeline().toImage(hist);
        Path out1 = Path.of(System.getProperty("java.io.tmpdir"), filename);
        Files.createDirectories(out1.getParent());
        ImageIO.write(img, "png", new File(out1.toString()));
        System.out.println("[phase2-manual] wrote " + out1 + " max-count=" + hist.maxCount()
                + " total-count=" + hist.totalCount());
    }
}
