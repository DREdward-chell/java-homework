package com.edwards.gpucalc.backend.opencl;

import com.edwards.gpucalc.backend.BackendId;
import com.edwards.gpucalc.backend.RenderBackend;
import com.edwards.gpucalc.backend.RenderResult;
import com.edwards.gpucalc.backend.gpu.GpuRanging;
import com.edwards.gpucalc.backend.gpu.GpuRanging.Viewport;
import com.edwards.gpucalc.config.ResolvedConfig;
import com.edwards.gpucalc.config.WeightedVariationRef;
import com.edwards.gpucalc.core.AffineCoeffs;
import com.edwards.gpucalc.core.Histogram;
import com.edwards.gpucalc.core.Palette;
import com.edwards.gpucalc.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceInfo;
import static org.lwjgl.opencl.CL10.clGetProgramBuildInfo;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1f;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.system.MemoryStack.stackPush;

@Slf4j
@Component
@BackendId("opencl")
@Conditional(ClAvailable.class)
@RequiredArgsConstructor
public final class OpenClRenderBackend implements RenderBackend {

    static final String KERNEL_TEMPLATE = "/kernels/chaos_game.cl";
    static final String KERNEL_NAME = "chaos_game";
    static final float COLOR_SCALE = 1000.0f;
    static final int WARMUP_ITERATIONS = GpuRanging.WARMUP_ITERATIONS;
    static final int ITERATIONS_PER_INVOCATION = 1024;
    static final int PREFERRED_LOCAL_SIZE = 64;
    static final Map<String, Integer> VARIATION_IDS = Map.of(
            "linear", 0,
            "sinusoidal", 1,
            "spherical", 2,
            "swirl", 3,
            "horseshoe", 4);
    static final List<String> ALL_VARIATION_IDS = List.of(
            "linear", "sinusoidal", "spherical", "swirl", "horseshoe");

    private final MetricsService metrics;

    @Override
    public RenderResult render(ResolvedConfig config) {
        maybeWarnMacOs();
        long startNanos = System.nanoTime();
        int iterations = config.iterationCount();
        List<AffineCoeffs> affines = config.affineParams();
        List<WeightedVariationRef> variations = config.functions();
        Viewport view = GpuRanging.find(config, affines, variations);

        Histogram hist;
        try (ClContext ctx = ClContext.open(config.clPlatform(), config.clDevice())) {
            if (!ctx.deviceIsGpu()) {
                log.warn("OpenCL device '{}' is not a GPU — falling back to CPU-OpenCL. "
                        + "Performance will be limited.", ctx.deviceName());
            }
            hist = runOnCl(ctx, config, affines, variations, view);
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        metrics.recordRender(iterations, elapsed);
        log.info("opencl backend: done in {} ms; total-count={}, max-count={}",
                elapsed.toMillis(), hist.totalCount(), hist.maxCount());
        return new RenderResult(hist, iterations, elapsed, "opencl");
    }

    private Histogram runOnCl(ClContext ctx, ResolvedConfig config,
                              List<AffineCoeffs> affines, List<WeightedVariationRef> variations,
                              Viewport view) {
        int width = config.width();
        int height = config.height();
        int iterations = config.iterationCount();

        String source = new KernelSourceBuilder().build(KERNEL_TEMPLATE, ALL_VARIATION_IDS);

        long program = buildProgram(ctx, source);
        long kernel = 0L;
        long transformsBuf = 0L, variationsBuf = 0L, histBuf = 0L, paletteBuf = 0L;
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            kernel = clCreateKernel(program, KERNEL_NAME, errcode);
            ClContext.check(errcode.get(0), "clCreateKernel");

            transformsBuf = createTransformsBuffer(ctx, affines);
            variationsBuf = createVariationsBuffer(ctx, variations);
            paletteBuf = createPaletteBuffer(ctx);
            histBuf = createHistogramBuffer(ctx, width, height);

            setKernelArgs(kernel, transformsBuf, variationsBuf, histBuf, paletteBuf,
                    width, height, affines.size(), variations.size(), config, view);

            int totalInvocations = Math.max(1, iterations / ITERATIONS_PER_INVOCATION);
            int localSize = pickLocalSize(ctx);
            int globalSize = ((totalInvocations + localSize - 1) / localSize) * localSize;
            log.info("opencl: {}x{}, {} iter ({} invocations × {} iters/inv, local={}), "
                            + "platform='{}', device='{}'",
                    width, height, iterations, globalSize, ITERATIONS_PER_INVOCATION, localSize,
                    ctx.platformName(), ctx.deviceName());

            PointerBuffer gwo = stack.mallocPointer(1).put(0, 0L);
            PointerBuffer gws = stack.mallocPointer(1).put(0, globalSize);
            PointerBuffer lws = stack.mallocPointer(1).put(0, localSize);
            int err = clEnqueueNDRangeKernel(ctx.queue(), kernel, 1, gwo, gws, lws, null, null);
            ClContext.check(err, "clEnqueueNDRangeKernel");
            ClContext.check(clFinish(ctx.queue()), "clFinish");

            return readbackHistogram(ctx, histBuf, width, height);
        } finally {
            if (histBuf != 0L) clReleaseMemObject(histBuf);
            if (paletteBuf != 0L) clReleaseMemObject(paletteBuf);
            if (variationsBuf != 0L) clReleaseMemObject(variationsBuf);
            if (transformsBuf != 0L) clReleaseMemObject(transformsBuf);
            if (kernel != 0L) clReleaseKernel(kernel);
            clReleaseProgram(program);
        }
    }

    private static long buildProgram(ClContext ctx, String source) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            long program = clCreateProgramWithSource(ctx.context(), source, errcode);
            ClContext.check(errcode.get(0), "clCreateProgramWithSource");
            int err = clBuildProgram(program, ctx.device(), "-cl-std=CL1.2", null, 0L);
            if (err != CL_SUCCESS) {
                String log = readBuildLog(ctx, program);
                clReleaseProgram(program);
                throw new IllegalStateException(
                        "clBuildProgram failed with code " + err + ":\n" + log);
            }
            return program;
        }
    }

    private static String readBuildLog(ClContext ctx, long program) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer size = stack.mallocPointer(1);
            int err = clGetProgramBuildInfo(program, ctx.device(), CL_PROGRAM_BUILD_LOG,
                    (ByteBuffer) null, size);
            if (err != CL_SUCCESS) return "(unable to read build log, code=" + err + ")";
            int n = (int) size.get(0);
            if (n <= 0) return "(empty build log)";
            ByteBuffer buf = BufferUtils.createByteBuffer(n);
            err = clGetProgramBuildInfo(program, ctx.device(), CL_PROGRAM_BUILD_LOG, buf, null);
            if (err != CL_SUCCESS) return "(unable to read build log data, code=" + err + ")";
            byte[] bytes = new byte[Math.max(0, n - 1)];
            buf.get(bytes);
            return new String(bytes);
        }
    }

    private static long createTransformsBuffer(ClContext ctx, List<AffineCoeffs> affines) {
        FloatBuffer data = BufferUtils.createFloatBuffer(affines.size() * 8);
        int last = Math.max(affines.size() - 1, 1);
        for (int i = 0; i < affines.size(); i++) {
            AffineCoeffs a = affines.get(i);
            data.put((float) a.a()).put((float) a.b()).put((float) a.c())
                .put((float) a.d()).put((float) a.e()).put((float) a.f());
            float c = affines.size() == 1 ? 0.5f : (float) i / last;
            data.put(c).put(0.0f);
        }
        data.flip();
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            long buf = clCreateBuffer(ctx.context(),
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, data, errcode);
            ClContext.check(errcode.get(0), "clCreateBuffer(transforms)");
            return buf;
        }
    }

    private static long createVariationsBuffer(ClContext ctx, List<WeightedVariationRef> variations) {
        IntBuffer data = BufferUtils.createIntBuffer(variations.size() * 2);
        for (WeightedVariationRef ref : variations) {
            Integer id = VARIATION_IDS.get(ref.name());
            if (id == null) {
                throw new IllegalStateException(
                        "variation '" + ref.name() + "' not supported by opencl backend; "
                                + "supported: " + VARIATION_IDS.keySet());
            }
            data.put(id);
        }
        for (WeightedVariationRef ref : variations) {
            data.put(Float.floatToRawIntBits((float) ref.weight()));
        }
        data.flip();
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            long buf = clCreateBuffer(ctx.context(),
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, data, errcode);
            ClContext.check(errcode.get(0), "clCreateBuffer(variations)");
            return buf;
        }
    }

    private static long createPaletteBuffer(ClContext ctx) {
        Palette p = Palette.defaultPalette();
        FloatBuffer data = BufferUtils.createFloatBuffer(256 * 4);
        double[] rgb = new double[3];
        for (int i = 0; i < 256; i++) {
            p.sample(i / 255.0, rgb);
            data.put((float) rgb[0]).put((float) rgb[1]).put((float) rgb[2]).put(0.0f);
        }
        data.flip();
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            long buf = clCreateBuffer(ctx.context(),
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, data, errcode);
            ClContext.check(errcode.get(0), "clCreateBuffer(palette)");
            return buf;
        }
    }

    private static long createHistogramBuffer(ClContext ctx, int width, int height) {
        int n = width * height * 4;
        IntBuffer zero = BufferUtils.createIntBuffer(n);
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode = stack.mallocInt(1);
            long buf = clCreateBuffer(ctx.context(), CL_MEM_READ_WRITE, (long) n * 4L, errcode);
            ClContext.check(errcode.get(0), "clCreateBuffer(histogram)");
            int err = clEnqueueWriteBuffer(ctx.queue(), buf, true, 0L, zero, null, null);
            ClContext.check(err, "clEnqueueWriteBuffer(zero histogram)");
            return buf;
        }
    }

    private static void setKernelArgs(long kernel,
                                      long transformsBuf, long variationsBuf,
                                      long histBuf, long paletteBuf,
                                      int width, int height, int numTransforms, int numVariations,
                                      ResolvedConfig config, Viewport view) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer one = stack.mallocPointer(1);
            ClContext.check(clSetKernelArg(kernel, 0, one.put(0, transformsBuf)),
                    "clSetKernelArg(0 transforms)");
            ClContext.check(clSetKernelArg(kernel, 1, one.put(0, variationsBuf)),
                    "clSetKernelArg(1 variations)");
            ClContext.check(clSetKernelArg(kernel, 2, one.put(0, histBuf)),
                    "clSetKernelArg(2 hist)");
            ClContext.check(clSetKernelArg(kernel, 3, one.put(0, paletteBuf)),
                    "clSetKernelArg(3 palette)");
            ClContext.check(clSetKernelArg1i(kernel, 4, width), "clSetKernelArg(width)");
            ClContext.check(clSetKernelArg1i(kernel, 5, height), "clSetKernelArg(height)");
            ClContext.check(clSetKernelArg1i(kernel, 6, numTransforms), "clSetKernelArg(numTransforms)");
            ClContext.check(clSetKernelArg1i(kernel, 7, numVariations), "clSetKernelArg(numVariations)");
            ClContext.check(clSetKernelArg1i(kernel, 8, ITERATIONS_PER_INVOCATION),
                    "clSetKernelArg(iterations)");
            ClContext.check(clSetKernelArg1i(kernel, 9, WARMUP_ITERATIONS),
                    "clSetKernelArg(warmup)");
            long masterSeed = Double.doubleToLongBits(config.seed());
            ClContext.check(clSetKernelArg1i(kernel, 10, (int) (masterSeed & 0xFFFFFFFFL)),
                    "clSetKernelArg(seedLo)");
            ClContext.check(clSetKernelArg1i(kernel, 11, (int) (masterSeed >>> 32)),
                    "clSetKernelArg(seedHi)");
            ClContext.check(clSetKernelArg1f(kernel, 12, (float) view.minX()),
                    "clSetKernelArg(viewMinX)");
            ClContext.check(clSetKernelArg1f(kernel, 13,
                            (float) (1.0 / (view.maxX() - view.minX()))),
                    "clSetKernelArg(viewInvRangeX)");
            ClContext.check(clSetKernelArg1f(kernel, 14, (float) view.minY()),
                    "clSetKernelArg(viewMinY)");
            ClContext.check(clSetKernelArg1f(kernel, 15,
                            (float) (1.0 / (view.maxY() - view.minY()))),
                    "clSetKernelArg(viewInvRangeY)");
            ClContext.check(clSetKernelArg1f(kernel, 16, COLOR_SCALE),
                    "clSetKernelArg(colorScale)");
        }
    }

    private static Histogram readbackHistogram(ClContext ctx, long histBuf, int width, int height) {
        int n = width * height;
        IntBuffer raw = BufferUtils.createIntBuffer(n * 4);
        int err = clEnqueueReadBuffer(ctx.queue(), histBuf, true, 0L, raw, null, null);
        ClContext.check(err, "clEnqueueReadBuffer(histogram)");

        Histogram out = new Histogram(width, height);
        long[] counts = out.counts();
        double[] r = out.redAccumulator();
        double[] g = out.greenAccumulator();
        double[] b = out.blueAccumulator();
        double invScale = 1.0 / COLOR_SCALE;
        for (int i = 0; i < n; i++) {
            int base = i * 4;
            counts[i] = Integer.toUnsignedLong(raw.get(base));
            r[i] = Integer.toUnsignedLong(raw.get(base + 1)) * invScale;
            g[i] = Integer.toUnsignedLong(raw.get(base + 2)) * invScale;
            b[i] = Integer.toUnsignedLong(raw.get(base + 3)) * invScale;
        }
        return out;
    }

    private static int pickLocalSize(ClContext ctx) {
        try (MemoryStack stack = stackPush()) {
            java.nio.LongBuffer v = stack.mallocLong(1);
            int err = clGetDeviceInfo(ctx.device(), CL_DEVICE_MAX_WORK_GROUP_SIZE, v, null);
            if (err != CL_SUCCESS) return 1;
            long max = v.get(0);
            if (max >= PREFERRED_LOCAL_SIZE) return PREFERRED_LOCAL_SIZE;
            return Math.max(1, (int) max);
        }
    }

    private static boolean isMacOs() {
        String name = System.getProperty("os.name", "").toLowerCase();
        return name.contains("mac") || name.contains("darwin");
    }

    private static void maybeWarnMacOs() {
        if (isMacOs()) {
            log.warn("OpenCL is deprecated on macOS (removed in a future macOS release). "
                    + "This backend still works through macOS 15.x. Consider the 'java' backend "
                    + "for long-term portability.");
        }
    }
}
