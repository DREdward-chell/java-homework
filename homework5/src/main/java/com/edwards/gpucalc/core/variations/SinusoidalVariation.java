package com.edwards.gpucalc.core.variations;

import com.edwards.gpucalc.core.Variation;
import com.edwards.gpucalc.core.VariationId;
import org.springframework.stereotype.Component;

@Component
@VariationId("sinusoidal")
public final class SinusoidalVariation implements Variation {
    @Override
    public void apply(double x, double y, double[] out) {
        out[0] = Math.sin(x);
        out[1] = Math.sin(y);
    }
}
