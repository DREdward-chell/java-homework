package com.edwards.gpucalc.core.variations;

import com.edwards.gpucalc.core.Variation;
import com.edwards.gpucalc.core.VariationId;
import org.springframework.stereotype.Component;

@Component
@VariationId("linear")
public final class LinearVariation implements Variation {
    @Override
    public void apply(double x, double y, double[] out) {
        out[0] = x;
        out[1] = y;
    }
}
