package com.edwards.gpucalc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsServiceTest {

    @Test
    void recordRenderUpdatesTimerAndCounter() {
        MetricsService m = new MetricsService();
        m.recordRender(1000, Duration.ofMillis(123));
        Counter iter = m.registry().find(MetricsService.COUNTER_RENDER_ITERATIONS).counter();
        Timer timer = m.registry().find(MetricsService.TIMER_RENDER_DURATION).timer();
        assertThat(iter).isNotNull();
        assertThat(iter.count()).isEqualTo(1000.0);
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void repeatedRecordsAccumulate() {
        MetricsService m = new MetricsService();
        m.recordRender(500, Duration.ofMillis(10));
        m.recordRender(300, Duration.ofMillis(20));
        Counter iter = m.registry().find(MetricsService.COUNTER_RENDER_ITERATIONS).counter();
        assertThat(iter.count()).isEqualTo(800.0);
    }

    @Test
    void dumpToLogEmptyIsSafe() {
        MetricsService m = new MetricsService();
        m.dumpToLog();
    }

    @Test
    void dumpToLogAfterRecordIsSafe() {
        MetricsService m = new MetricsService();
        m.recordRender(10, Duration.ofMillis(1));
        m.dumpToLog();
    }
}
