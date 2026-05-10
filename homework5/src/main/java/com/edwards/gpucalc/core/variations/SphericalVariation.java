package com.edwards.gpucalc.core.variations;

import com.edwards.gpucalc.core.Variation;
import com.edwards.gpucalc.core.VariationId;
import org.springframework.stereotype.Component;

@Component
@VariationId("spherical")
public final class SphericalVariation implements Variation {
    @Override
    public void apply(double x, double y, double[] out) {
        double r2 = x * x + y * y;
        if (r2 == 0.0) {
            out[0] = 0.0;
            out[1] = 0.0;
            return;
        }
        double inv = 1.0 / r2;
        out[0] = x * inv;
        out[1] = y * inv;
    }
}
