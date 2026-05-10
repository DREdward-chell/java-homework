package com.edwards.gpucalc.backend;

import com.edwards.gpucalc.core.Histogram;

import java.time.Duration;

public record RenderResult(
        Histogram histogram,
        long iterations,
        Duration duration,
        String backendId) {}
