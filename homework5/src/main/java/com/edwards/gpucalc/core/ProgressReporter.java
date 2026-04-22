package com.edwards.gpucalc.core;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class ProgressReporter {

    private final long total;
    private final String label;
    private final AtomicInteger lastBucket = new AtomicInteger(-1);

    public ProgressReporter(long total, String label) {
        if (total <= 0) throw new IllegalArgumentException("total must be > 0; got " + total);
        this.total = total;
        this.label = label;
    }

    public void report(long done) {
        int bucket = (int) (done * 20 / total);
        int observed = lastBucket.get();
        while (bucket > observed) {
            if (lastBucket.compareAndSet(observed, bucket)) {
                int pct = bucket * 5;
                log.info("{}: {}% ({}/{})", label, pct, done, total);
                return;
            }
            observed = lastBucket.get();
        }
    }

    public void complete() {
        if (lastBucket.getAndSet(20) < 20) {
            log.info("{}: 100% ({}/{})", label, total, total);
        }
    }
}
