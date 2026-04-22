package com.edwards.gpucalc.backend.opencl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KernelSourceBuilder {

    static final String INCLUDE_SENTINEL = "//#include \"variations\"";

    private final ClassLoader classLoader;

    public KernelSourceBuilder() {
        this(KernelSourceBuilder.class.getClassLoader());
    }

    KernelSourceBuilder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String build(String templateResourcePath, List<String> variationIds) {
        String template = loadResource(templateResourcePath);
        if (!template.contains(INCLUDE_SENTINEL)) {
            throw new IllegalStateException(
                    "kernel template " + templateResourcePath + " is missing "
                            + INCLUDE_SENTINEL + " sentinel");
        }
        StringBuilder library = new StringBuilder();
        library.append(translateToCl(loadResource("/variations/common.inc"))).append('\n');
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String id : variationIds) {
            if (seen.putIfAbsent(id, true) != null) continue;
            library.append(translateToCl(loadResource("/variations/" + id + ".inc"))).append('\n');
        }
        return template.replace(INCLUDE_SENTINEL, library.toString());
    }

    static String translateToCl(String glslSource) {
        return glslSource
                .replace("vec2(", "(float2)(")
                .replace("vec2 ", "float2 ");
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
