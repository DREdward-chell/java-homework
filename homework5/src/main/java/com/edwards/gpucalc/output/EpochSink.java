package com.edwards.gpucalc.output;

import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.Histogram;

import java.io.IOException;

public interface EpochSink extends OutputSink {

    void beginEpochs(ResolvedConfig config) throws IOException;

    void writeEpoch(Histogram snapshot, int index, int totalEpochs, ResolvedConfig config)
            throws IOException;

    void endEpochs(ResolvedConfig config) throws IOException;

    @Override
    default void write(RenderResult result, ResolvedConfig config) throws IOException {
        beginEpochs(config);
        writeEpoch(result.histogram(), 1, 1, config);
        endEpochs(config);
    }
}
