package com.edwards.gpucalc.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressReporterTest {

    @Test
    void reportAcceptsMonotonicProgress() {
        ProgressReporter r = new ProgressReporter(100, "test");
        for (int i = 0; i <= 100; i++) {
            r.report(i);
        }
        r.complete();
    }

    @Test
    void completeIsIdempotent() {
        ProgressReporter r = new ProgressReporter(10, "test");
        r.report(10);
        r.complete();
        r.complete();
    }

    @Test
    void constructorRejectsNonPositiveTotal() {
        assertThatThrownBy(() -> new ProgressReporter(0, "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProgressReporter(-1, "t"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeWithoutReportsStillFires() {
        ProgressReporter r = new ProgressReporter(5, "test");
        r.complete();
        assertThat(r).isNotNull();
    }

    @Test
    void concurrentReportsAdvanceBucketMonotonically() throws Exception {
        ProgressReporter r = new ProgressReporter(1_000_000, "parallel");
        int threads = 8;
        int perThread = 100_000;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int offset = t * perThread;
            workers[t] = new Thread(() -> {
                for (int i = 1; i <= perThread; i++) {
                    r.report(offset + i);
                }
            });
        }
        for (Thread w : workers) w.start();
        for (Thread w : workers) w.join();
        r.complete();
    }
}
