package com.edwards.gpucalc.backend.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles GLSL compute shader source by inlining the shared variation library (CLAUDE.md §3.5).
 * <p>
 * The compute shader template contains a single sentinel comment
 * {@code //#include "variations"} — at load time this builder replaces that sentinel with the
 * concatenated contents of {@code variations/common.inc} followed by each requested
 * {@code variations/<id>.inc}. The result is a self-contained shader source string ready for
 * {@code glShaderSource}.
 * <p>
 * This is the mechanism that lets the Java, OpenGL and OpenCL backends share variation
 * definitions without code duplication. Future phases reuse this builder for the OpenCL kernel
 * (with a different template).
 */
public final class ShaderSourceBuilder {

    static final String INCLUDE_SENTINEL = "//#include \"variations\"";

    private final ClassLoader classLoader;

    public ShaderSourceBuilder() {
        this(ShaderSourceBuilder.class.getClassLoader());
    }

    ShaderSourceBuilder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Load the compute shader template at the given classpath path, splice in the variation
     * library, and return the full source. {@code variationIds} is iterated in order; duplicates
     * are silently ignored so callers do not have to de-duplicate a config list.
     */
    public String build(String templateResourcePath, List<String> variationIds) {
        String template = loadResource(templateResourcePath);
        if (!template.contains(INCLUDE_SENTINEL)) {
            throw new IllegalStateException(
                    "shader template " + templateResourcePath + " is missing "
                            + INCLUDE_SENTINEL + " sentinel");
        }
        StringBuilder library = new StringBuilder();
        library.append(loadResource("/variations/common.inc")).append('\n');
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String id : variationIds) {
            if (seen.putIfAbsent(id, true) != null) continue;
            library.append(loadResource("/variations/" + id + ".inc")).append('\n');
        }
        return template.replace(INCLUDE_SENTINEL, library.toString());
    }

    private String loadResource(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream in = classLoader.getResourceAsStream(normalized)) {
            if (in == null) {
                throw new IllegalStateException("classpath resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed reading " + path, e);
        }
    }
}
