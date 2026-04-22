package com.edwards.gpucalc.core;

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
public final class VariationRegistry {

    private final Map<String, Variation> byId;

    public VariationRegistry(ApplicationContext ctx) {
        Map<String, Variation> discovered = new LinkedHashMap<>();
        for (Variation bean : ctx.getBeansOfType(Variation.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            VariationId id = targetClass.getAnnotation(VariationId.class);
            if (id == null) {
                log.warn("Variation bean {} is missing @VariationId; ignoring", targetClass.getName());
                continue;
            }
            Variation previous = discovered.putIfAbsent(id.value(), bean);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate @VariationId(\"" + id.value() + "\"): "
                                + previous.getClass().getName() + " vs " + targetClass.getName());
            }
        }
        this.byId = Collections.unmodifiableMap(discovered);
        log.info("Registered {} variation(s): {}", byId.size(), byId.keySet());
    }

    public Optional<Variation> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Variation require(String id) {
        Variation v = byId.get(id);
        if (v == null) {
            throw new IllegalArgumentException("Unknown variation id: " + id);
        }
        return v;
    }

    public Set<String> ids() {
        return byId.keySet();
    }

    public int size() {
        return byId.size();
    }
}
