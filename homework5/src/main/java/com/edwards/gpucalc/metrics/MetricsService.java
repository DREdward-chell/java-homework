package com.edwards.gpucalc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public final class MetricsService {

    public static final String TIMER_RENDER_DURATION = "flame.render.duration";
    public static final String COUNTER_RENDER_ITERATIONS = "flame.render.iterations";

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final Timer renderDuration = registry.timer(TIMER_RENDER_DURATION);
    private final Counter renderIterations = registry.counter(COUNTER_RENDER_ITERATIONS);

    public void recordRender(long iterations, Duration duration) {
        renderDuration.record(duration);
        renderIterations.increment(iterations);
    }

    public MeterRegistry registry() {
        return registry;
    }

    public void dumpToLog() {
        List<Meter> meters = registry.getMeters();
        if (meters.isEmpty()) {
            log.info("metrics: (no meters recorded)");
            return;
        }
        StringBuilder sb = new StringBuilder("metrics:\n");
        for (Meter m : meters) {
            sb.append("  ").append(m.getId().getName()).append(":");
            for (Measurement meas : m.measure()) {
                sb.append(" ").append(meas.getStatistic()).append("=").append(meas.getValue());
            }
            sb.append("\n");
        }
        log.info(sb.toString().stripTrailing());
    }
}
