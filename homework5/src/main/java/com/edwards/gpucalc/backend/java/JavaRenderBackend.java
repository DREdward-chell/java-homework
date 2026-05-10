package com.edwards.gpucalc.backend.java;

import com.edwards.gpucalc.backend.BackendId;
import com.edwards.gpucalc.backend.EpochConsumer;
import com.edwards.gpucalc.backend.RenderBackend;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.config.EpochConfig;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.core.Histogram;
import com.edwards.gpucalc.core.Palette;
import com.edwards.gpucalc.core.ProgressReporter;
import com.edwards.gpucalc.core.Transform;
import com.edwards.gpucalc.core.TransformFactory;
import com.edwards.gpucalc.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

@Slf4j
@Component
@BackendId("java")
@RequiredArgsConstructor
public final class JavaRenderBackend implements RenderBackend {

    static final int WARMUP_ITERATIONS = 20;
    static final int RANGING_ITERATIONS = 10_000;
    static final int PROGRESS_BATCH = 10_000;
    private static final long SEED_MIX = 0x9E3779B97F4A7C15L;

    private final TransformFactory transformFactory;
    private final MetricsService metrics;

    @Override
    public RenderResult render(ResolvedConfig config) {
        return renderInternal(config, null);
    }

    @Override
    public RenderResult renderEpochs(ResolvedConfig config, EpochConsumer epochs) {
        if (config.epochs() == null) {
            throw new IllegalStateException(
                    "renderEpochs called without epochs configured in ResolvedConfig");
        }
        return renderInternal(config, epochs);
    }

    private RenderResult renderInternal(ResolvedConfig config, EpochConsumer epochs) {
        int threads = Math.max(1, config.threads());
        Transform[] transforms = transformFactory.build(config);
        long masterSeed = Double.doubleToLongBits(config.seed());
        int width = config.width();
        int height = config.height();
        int iterations = config.iterationCount();

        long stripBytes = (long) width * height * 32L;
        log.info("java backend: {}x{}, {} iter, {} transform(s), {} thread(s), seed={} (strip memory {} MiB/thread)",
                width, height, iterations, transforms.length, threads, config.seed(),
                stripBytes / (1024L * 1024L));

        long startNanos = System.nanoTime();
        Viewport view = findViewport(transforms, masterSeed);

        Histogram hist;
        if (epochs != null && config.epochs() != null) {
            hist = runEpochPass(config, transforms, view, masterSeed, threads, epochs);
        } else {
            hist = runMainPass(config, transforms, view, masterSeed, threads);
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        metrics.recordRender(iterations, elapsed);
        log.info("java backend: done in {} ms; total-count={}, max-count={}",
                elapsed.toMillis(), hist.totalCount(), hist.maxCount());

        return new RenderResult(hist, iterations, elapsed, "java");
    }

    private Viewport findViewport(Transform[] transforms, long masterSeed) {
        RandomGenerator rng = rngFor(masterSeed ^ SEED_MIX);
        double x = 0.0, y = 0.0;
        double[] scratch = new double[2];
        double[] out = new double[2];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < RANGING_ITERATIONS + WARMUP_ITERATIONS; i++) {
            Transform t = transforms[rng.nextInt(transforms.length)];
            t.apply(x, y, scratch, out);
            x = out[0];
            y = out[1];
            if (i < WARMUP_ITERATIONS) continue;
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        if (!Double.isFinite(minX) || minX == maxX || minY == maxY) {
            return new Viewport(-1, 1, -1, 1);
        }
        double padX = (maxX - minX) * 0.02;
        double padY = (maxY - minY) * 0.02;
        return new Viewport(minX - padX, maxX + padX, minY - padY, maxY + padY);
    }

    private Histogram runMainPass(ResolvedConfig config, Transform[] transforms,
                                  Viewport view, long masterSeed, int threads) {
        int iterations = config.iterationCount();
        ProgressReporter progress = new ProgressReporter(iterations, "java-render");
        AtomicLong plottedTotal = new AtomicLong();

        if (threads == 1) {
            FlameWorker worker = new FlameWorker(config, transforms, view,
                    seedFor(masterSeed, 0), progress, plottedTotal);
            worker.advance(iterations);
            progress.complete();
            return worker.strip();
        }

        int baseIter = iterations / threads;
        int remainder = iterations - baseIter * threads;


        List<Future<Histogram>> futures = new ArrayList<>(threads);
        try(ExecutorService pool = Executors.newFixedThreadPool(threads, new FlameWorkerThreadFactory())) {
            for (int ti = 0; ti < threads; ti++) {
                int iters = baseIter + (ti == 0 ? remainder : 0);
                long seed = seedFor(masterSeed, ti);
                futures.add(pool.submit(() -> {
                    FlameWorker worker = new FlameWorker(config, transforms, view,
                            seed, progress, plottedTotal);
                    worker.advance(iters);
                    return worker.strip();
                }));
            }
            Histogram merged = new Histogram(config.width(), config.height());
            for (Future<Histogram> f : futures) {
                merged.mergeFrom(f.get());
            }
            progress.complete();
            return merged;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("render interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException rex) throw rex;
            throw new IllegalStateException("worker failure", cause);
        }
    }

    private Histogram runEpochPass(ResolvedConfig config, Transform[] transforms,
                                   Viewport view, long masterSeed, int threads,
                                   EpochConsumer epochs) {
        EpochConfig ec = config.epochs();
        int iterations = config.iterationCount();
        int totalEpochs = ec.epochCount();
        int stride = ec.epochStride();
        int consumed = totalEpochs * stride;
        if (consumed > iterations) {
            throw new IllegalStateException(
                    "epoch-count × epoch-stride (" + consumed + ") exceeds iteration-count (" + iterations + ")");
        }
        int tail = iterations - consumed;

        ProgressReporter progress = new ProgressReporter(iterations, "java-render");
        AtomicLong plottedTotal = new AtomicLong();

        FlameWorker[] workers = new FlameWorker[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new FlameWorker(config, transforms, view,
                    seedFor(masterSeed, i), progress, plottedTotal);
        }

        ExecutorService pool = threads > 1
                ? Executors.newFixedThreadPool(threads, new FlameWorkerThreadFactory())
                : null;
        Histogram lastSnapshot = null;
        try {
            for (int e = 1; e <= totalEpochs; e++) {
                int stepIter = stride + (e == totalEpochs ? tail : 0);
                advanceWorkers(workers, stepIter, pool);
                Histogram snapshot = new Histogram(config.width(), config.height());
                for (FlameWorker w : workers) {
                    snapshot.mergeFrom(w.strip());
                }
                epochs.accept(snapshot, e, totalEpochs);
                lastSnapshot = snapshot;
            }
            progress.complete();
            return lastSnapshot;
        } finally {
            if (pool != null) pool.shutdown();
        }
    }

    private static void advanceWorkers(FlameWorker[] workers, int totalIters, ExecutorService pool) {
        int threads = workers.length;
        int base = totalIters / threads;
        int rem = totalIters - base * threads;
        if (pool == null) {
            workers[0].advance(base + rem);
            return;
        }
        List<Future<?>> futures = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            FlameWorker w = workers[i];
            int iters = base + (i == 0 ? rem : 0);
            futures.add(pool.submit(() -> w.advance(iters)));
        }
        try {
            for (Future<?> f : futures) f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("epoch worker interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException rex) throw rex;
            throw new IllegalStateException("epoch worker failure", cause);
        }
    }

    private static long seedFor(long masterSeed, int threadIndex) {
        long mixed = masterSeed + threadIndex * SEED_MIX;
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);
        return mixed;
    }

    static RandomGenerator rngFor(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    record Viewport(double minX, double maxX, double minY, double maxY) {}

    static final class FlameWorker {
        private final ResolvedConfig config;
        private final Transform[] transforms;
        private final Viewport view;
        private final Histogram strip;
        private final RandomGenerator rng;
        private final Palette palette = Palette.defaultPalette();
        private final ProgressReporter progress;
        private final AtomicLong plottedTotal;
        private final double[] scratch = new double[2];
        private final double[] tmp = new double[2];
        private final double[] rgb = new double[3];

        private double x = 0.0, y = 0.0, c = 0.5;
        private boolean warmedUp = false;

        FlameWorker(ResolvedConfig config, Transform[] transforms, Viewport view,
                    long seed, ProgressReporter progress, AtomicLong plottedTotal) {
            this.config = config;
            this.transforms = transforms;
            this.view = view;
            this.strip = new Histogram(config.width(), config.height());
            this.rng = rngFor(seed);
            this.progress = progress;
            this.plottedTotal = plottedTotal;
        }

        Histogram strip() {
            return strip;
        }

        void advance(int iterations) {
            if (iterations <= 0) return;
            if (!warmedUp) {
                for (int i = 0; i < WARMUP_ITERATIONS; i++) stepOnly();
                warmedUp = true;
            }
            double invRangeX = 1.0 / (view.maxX - view.minX);
            double invRangeY = 1.0 / (view.maxY - view.minY);
            int w = config.width();
            int h = config.height();
            for (int i = 0; i < iterations; i++) {
                stepOnly();
                if (!Double.isFinite(x) || !Double.isFinite(y)) {
                    maybeProgress(i);
                    continue;
                }
                int px = (int) ((x - view.minX) * invRangeX * (w - 1));
                int py = (int) ((1.0 - (y - view.minY) * invRangeY) * (h - 1));
                if (strip.inBounds(px, py)) {
                    palette.sample(c, rgb);
                    strip.plot(px, py, rgb[0], rgb[1], rgb[2]);
                }
                maybeProgress(i);
            }
        }

        private void maybeProgress(int localIdx) {
            if ((localIdx + 1) % PROGRESS_BATCH == 0) {
                progress.report(plottedTotal.addAndGet(PROGRESS_BATCH));
            }
        }

        private void stepOnly() {
            Transform t = transforms[rng.nextInt(transforms.length)];
            t.apply(x, y, scratch, tmp);
            x = tmp[0];
            y = tmp[1];
            c = (c + t.colorCoord()) * 0.5;
        }
    }
}
