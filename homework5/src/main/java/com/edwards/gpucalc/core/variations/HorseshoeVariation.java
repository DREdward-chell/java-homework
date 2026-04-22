package com.edwards.gpucalc.core.variations;

import com.edwards.gpucalc.core.Variation;
import com.edwards.gpucalc.core.VariationId;
import org.springframework.stereotype.Component;

@Component
@VariationId("horseshoe")
public final class HorseshoeVariation implements Variation {
    @Override
    public void apply(double x, double y, double[] out) {
        double r = Math.sqrt(x * x + y * y);
        if (r == 0.0) {
            out[0] = 0.0;
            out[1] = 0.0;
            return;
        }
        double inv = 1.0 / r;
        out[0] = (x - y) * (x + y) * inv;
        out[1] = 2.0 * x * y * inv;
    }
}
