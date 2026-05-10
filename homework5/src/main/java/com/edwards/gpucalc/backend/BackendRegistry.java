package com.edwards.gpucalc.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public final class BackendRegistry {

    private final Map<String, RenderBackend> byId;

    public BackendRegistry(ApplicationContext ctx) {
        Map<String, RenderBackend> discovered = new LinkedHashMap<>();
        for (RenderBackend bean : ctx.getBeansOfType(RenderBackend.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BackendId id = targetClass.getAnnotation(BackendId.class);
            if (id == null) {
                log.warn("RenderBackend bean {} is missing @BackendId; ignoring", targetClass.getName());
                continue;
            }
            RenderBackend previous = discovered.putIfAbsent(id.value(), bean);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate @BackendId(\"" + id.value() + "\"): "
                                + previous.getClass().getName() + " vs " + targetClass.getName());
            }
        }
        this.byId = Collections.unmodifiableMap(discovered);
        log.info("Registered {} backend(s): {}", byId.size(), byId.keySet());
    }

    public Optional<RenderBackend> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public RenderBackend require(String id) {
        RenderBackend b = byId.get(id);
        if (b == null) {
            if ("opengl-compute".equals(id) && isMacOs()) {
                throw new IllegalArgumentException(
                        "backend 'opengl-compute' is not supported on macOS "
                                + "(Apple deprecated OpenGL at 4.1 Core, predating compute shaders). "
                                + "Use '-b java' or '-b opencl' instead.");
            }
            throw new IllegalArgumentException(
                    "Unknown backend id '" + id + "'. Registered: " + byId.keySet());
        }
        return b;
    }

    private static boolean isMacOs() {
        String name = System.getProperty("os.name", "").toLowerCase();
        return name.contains("mac") || name.contains("darwin");
    }

    public Set<String> ids() {
        return byId.keySet();
    }
}
