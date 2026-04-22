package com.edwards.gpucalc.core.variations;

import com.edwards.gpucalc.core.Variation;
import com.edwards.gpucalc.core.VariationId;
import org.springframework.stereotype.Component;

@Component
@VariationId("swirl")
public final class SwirlVariation implements Variation {
    @Override
    public void apply(double x, double y, double[] out) {
        double r2 = x * x + y * y;
        double s = Math.sin(r2);
        double c = Math.cos(r2);
        out[0] = x * s - y * c;
        out[1] = x * c + y * s;
    }
}
