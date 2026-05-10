package com.edwards.gpucalc.backend.opengl;

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
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glUniform1ui;
import static org.lwjgl.opengl.GL43C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43C.GL_DYNAMIC_COPY;
import static org.lwjgl.opengl.GL43C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43C.GL_STATIC_READ;
import static org.lwjgl.opengl.GL43C.glAttachShader;
import static org.lwjgl.opengl.GL43C.glBindBuffer;
import static org.lwjgl.opengl.GL43C.glBindBufferBase;
import static org.lwjgl.opengl.GL43C.glBufferData;
import static org.lwjgl.opengl.GL43C.glCompileShader;
import static org.lwjgl.opengl.GL43C.glCreateProgram;
import static org.lwjgl.opengl.GL43C.glCreateShader;
import static org.lwjgl.opengl.GL43C.glDeleteBuffers;
import static org.lwjgl.opengl.GL43C.glDeleteProgram;
import static org.lwjgl.opengl.GL43C.glDeleteShader;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;
import static org.lwjgl.opengl.GL43C.glGenBuffers;
import static org.lwjgl.opengl.GL43C.glGetBufferSubData;
import static org.lwjgl.opengl.GL43C.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL43C.glGetProgrami;
import static org.lwjgl.opengl.GL43C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL43C.glGetShaderi;
import static org.lwjgl.opengl.GL43C.glLinkProgram;
import static org.lwjgl.opengl.GL43C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43C.glShaderSource;

@Slf4j
@Component
@BackendId("opengl-compute")
@Conditional(OpenGlAvailable.class)
@RequiredArgsConstructor
public final class OpenGlComputeRenderBackend implements RenderBackend {

    static final String SHADER_TEMPLATE = "/shaders/compute/chaos_game.comp";
    static final float COLOR_SCALE = 1000.0f;
    static final int WARMUP_ITERATIONS = GpuRanging.WARMUP_ITERATIONS;
    static final int LOCAL_SIZE_X = 64;
    static final int ITERATIONS_PER_INVOCATION = 1024;
    static final Map<String, Integer> VARIATION_IDS = Map.of(
            "linear", 0,
            "sinusoidal", 1,
            "spherical", 2,
            "swirl", 3,
            "horseshoe", 4);

    private final MetricsService metrics;

    @Override
    public RenderResult render(ResolvedConfig config) {
        long startNanos = System.nanoTime();
        int iterations = config.iterationCount();
        List<AffineCoeffs> affines = config.affineParams();
        List<WeightedVariationRef> variations = config.functions();

        Viewport view = GpuRanging.find(config, affines, variations);
        Histogram hist;
        try (GlContext ignored = GlContext.createHeadless()) {
            hist = runOnGpu(config, affines, variations, view);
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        metrics.recordRender(iterations, elapsed);
        log.info("opengl-compute backend: done in {} ms; total-count={}, max-count={}",
                elapsed.toMillis(), hist.totalCount(), hist.maxCount());
        return new RenderResult(hist, iterations, elapsed, "opengl-compute");
    }

    private Histogram runOnGpu(ResolvedConfig config,
                               List<AffineCoeffs> affines,
                               List<WeightedVariationRef> variations,
                               Viewport view) {
        int width = config.width();
        int height = config.height();
        int iterations = config.iterationCount();

        String source = new ShaderSourceBuilder()
                .build(SHADER_TEMPLATE, variations.stream().map(WeightedVariationRef::name).toList());

        int program = compileProgram(source);
        int transformsSsbo = 0;
        int variationsSsbo = 0;
        int histogramSsbo = 0;
        int paletteSsbo = 0;
        try {
            transformsSsbo = createTransformsBuffer(affines);
            variationsSsbo = createVariationsBuffer(variations);
            histogramSsbo = createHistogramBuffer(width, height);
            paletteSsbo = createPaletteBuffer();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, transformsSsbo);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, variationsSsbo);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, histogramSsbo);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, paletteSsbo);

            glUseProgram(program);
            long masterSeed = Double.doubleToLongBits(config.seed());
            setUniforms(program, width, height, affines.size(), variations.size(), view, masterSeed);

            int totalInvocations = Math.max(1, iterations / ITERATIONS_PER_INVOCATION);
            int groups = (totalInvocations + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
            long stripMiB = (long) width * height * 16L / (1024L * 1024L);
            log.info("opengl-compute: {}x{}, {} iter ({} groups × {} threads × {} iters/inv), "
                            + "histogram {} MiB",
                    width, height, iterations, groups, LOCAL_SIZE_X, ITERATIONS_PER_INVOCATION,
                    stripMiB);

            glDispatchCompute(groups, 1, 1);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

            return readbackHistogram(histogramSsbo, width, height);
        } finally {
            if (transformsSsbo != 0) glDeleteBuffers(transformsSsbo);
            if (variationsSsbo != 0) glDeleteBuffers(variationsSsbo);
            if (histogramSsbo != 0) glDeleteBuffers(histogramSsbo);
            if (paletteSsbo != 0) glDeleteBuffers(paletteSsbo);
            glUseProgram(0);
            glDeleteProgram(program);
        }
    }

    private static int compileProgram(String source) {
        int shader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("compute shader compile failed:\n" + log);
        }
        int program = glCreateProgram();
        glAttachShader(program, shader);
        glLinkProgram(program);
        glDeleteShader(shader);
        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new IllegalStateException("compute program link failed:\n" + log);
        }
        return program;
    }

    private static int createTransformsBuffer(List<AffineCoeffs> affines) {
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
        int ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_STATIC_READ);
        return ssbo;
    }

    private static int createVariationsBuffer(List<WeightedVariationRef> variations) {
        IntBuffer data = BufferUtils.createIntBuffer(variations.size() * 2);
        for (WeightedVariationRef ref : variations) {
            Integer id = VARIATION_IDS.get(ref.name());
            if (id == null) {
                throw new IllegalStateException(
                        "variation '" + ref.name() + "' not supported by opengl-compute backend; "
                                + "supported: " + VARIATION_IDS.keySet());
            }
            data.put(id);
        }
        for (WeightedVariationRef ref : variations) {
            data.put(Float.floatToRawIntBits((float) ref.weight()));
        }
        data.flip();
        int ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_STATIC_READ);
        return ssbo;
    }

    private static int createHistogramBuffer(int width, int height) {
        // 4 uints per pixel (count + rgb), allocated pre-zeroed so atomicAdd starts clean.
        // BufferUtils returns a zero-initialized direct ByteBuffer.
        int n = width * height * 4;
        ByteBuffer zero = BufferUtils.createByteBuffer(n * 4);
        int ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, zero, GL_DYNAMIC_COPY);
        return ssbo;
    }

    private static int createPaletteBuffer() {
        Palette p = Palette.defaultPalette();
        FloatBuffer data = BufferUtils.createFloatBuffer(256 * 4);
        double[] rgb = new double[3];
        for (int i = 0; i < 256; i++) {
            p.sample(i / 255.0, rgb);
            data.put((float) rgb[0]).put((float) rgb[1]).put((float) rgb[2]).put(0.0f);
        }
        data.flip();
        int ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_STATIC_READ);
        return ssbo;
    }

    private static void setUniforms(int program, int width, int height, int numTransforms,
                                    int numVariations, Viewport view, long masterSeed) {
        glUniform1ui(glGetUniformLocation(program, "u_width"), width);
        glUniform1ui(glGetUniformLocation(program, "u_height"), height);
        glUniform1ui(glGetUniformLocation(program, "u_numTransforms"), numTransforms);
        glUniform1ui(glGetUniformLocation(program, "u_numVariations"), numVariations);
        glUniform1ui(glGetUniformLocation(program, "u_iterationsPerInvocation"), ITERATIONS_PER_INVOCATION);
        glUniform1ui(glGetUniformLocation(program, "u_warmupIterations"), WARMUP_ITERATIONS);
        glUniform1ui(glGetUniformLocation(program, "u_seedLo"), (int) (masterSeed & 0xFFFFFFFFL));
        glUniform1ui(glGetUniformLocation(program, "u_seedHi"), (int) (masterSeed >>> 32));
        glUniform1f(glGetUniformLocation(program, "u_viewMinX"), (float) view.minX());
        glUniform1f(glGetUniformLocation(program, "u_viewInvRangeX"), (float) (1.0 / (view.maxX() - view.minX())));
        glUniform1f(glGetUniformLocation(program, "u_viewMinY"), (float) view.minY());
        glUniform1f(glGetUniformLocation(program, "u_viewInvRangeY"), (float) (1.0 / (view.maxY() - view.minY())));
        glUniform1f(glGetUniformLocation(program, "u_colorScale"), COLOR_SCALE);
    }

    private static Histogram readbackHistogram(int ssbo, int width, int height) {
        int n = width * height;
        ByteBuffer raw = BufferUtils.createByteBuffer(n * 4 * 4);
        raw.order(ByteOrder.nativeOrder());
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, raw);
        IntBuffer asInts = raw.asIntBuffer();

        Histogram out = new Histogram(width, height);
        long[] counts = out.counts();
        double[] r = out.redAccumulator();
        double[] g = out.greenAccumulator();
        double[] b = out.blueAccumulator();
        double invScale = 1.0 / COLOR_SCALE;
        for (int i = 0; i < n; i++) {
            int base = i * 4;
            counts[i] = Integer.toUnsignedLong(asInts.get(base));
            r[i] = Integer.toUnsignedLong(asInts.get(base + 1)) * invScale;
            g[i] = Integer.toUnsignedLong(asInts.get(base + 2)) * invScale;
            b[i] = Integer.toUnsignedLong(asInts.get(base + 3)) * invScale;
        }
        return out;
    }

}
