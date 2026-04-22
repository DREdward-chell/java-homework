package com.edwards.gpucalc.backend;

import com.edwards.gpucalc.config.ResolvedConfig;

public interface RenderBackend {

    RenderResult render(ResolvedConfig config);

    default RenderResult renderEpochs(ResolvedConfig config, EpochConsumer epochs) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support epoch rendering");
    }
}
