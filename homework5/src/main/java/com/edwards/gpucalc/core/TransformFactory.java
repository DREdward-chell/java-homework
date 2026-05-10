package com.edwards.gpucalc.core;

import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class TransformFactory {

    private final VariationRegistry variations;

    public TransformFactory(VariationRegistry variations) {
        this.variations = variations;
    }

    public Transform[] build(ResolvedConfig config) {
        List<AffineCoeffs> affines = config.affineParams();
        List<WeightedVariationRef> fns = config.functions();
        if (affines.isEmpty()) {
            throw new IllegalStateException("ResolvedConfig has no affine transforms");
        }
        if (fns.isEmpty()) {
            throw new IllegalStateException("ResolvedConfig has no variation functions");
        }

        Variation[] blend = new Variation[fns.size()];
        double[] weights = new double[fns.size()];
        for (int i = 0; i < fns.size(); i++) {
            WeightedVariationRef ref = fns.get(i);
            blend[i] = variations.require(ref.name());
            weights[i] = ref.weight();
        }

        Transform[] out = new Transform[affines.size()];
        int last = Math.max(affines.size() - 1, 1);
        for (int i = 0; i < affines.size(); i++) {
            double c = affines.size() == 1 ? 0.5 : (double) i / last;
            out[i] = new Transform(affines.get(i), blend, weights, c);
        }
        return out;
    }
}
