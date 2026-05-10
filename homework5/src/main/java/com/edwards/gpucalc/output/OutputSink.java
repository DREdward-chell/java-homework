package com.edwards.gpucalc.output;

import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.ResolvedConfig;

import java.io.IOException;

public interface OutputSink {

    void write(RenderResult result, ResolvedConfig config) throws IOException;
}
