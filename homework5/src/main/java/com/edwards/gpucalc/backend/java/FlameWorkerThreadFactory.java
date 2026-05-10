package com.edwards.gpucalc.backend.java;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class FlameWorkerThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "flame-worker-" + counter.getAndIncrement());
        t.setDaemon(false);
        return t;
    }
}
