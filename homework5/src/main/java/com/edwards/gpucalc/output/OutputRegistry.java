package com.edwards.gpucalc.output;

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
public final class OutputRegistry {

    private final Map<String, OutputSink> byId;

    public OutputRegistry(ApplicationContext ctx) {
        Map<String, OutputSink> discovered = new LinkedHashMap<>();
        for (OutputSink bean : ctx.getBeansOfType(OutputSink.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            OutputId id = targetClass.getAnnotation(OutputId.class);
            if (id == null) {
                log.warn("OutputSink bean {} is missing @OutputId; ignoring", targetClass.getName());
                continue;
            }
            OutputSink previous = discovered.putIfAbsent(id.value(), bean);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate @OutputId(\"" + id.value() + "\"): "
                                + previous.getClass().getName() + " vs " + targetClass.getName());
            }
        }
        this.byId = Collections.unmodifiableMap(discovered);
        log.info("Registered {} output sink(s): {}", byId.size(), byId.keySet());
    }

    public Optional<OutputSink> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public OutputSink require(String id) {
        OutputSink s = byId.get(id);
        if (s == null) {
            throw new IllegalArgumentException(
                    "Unknown output sink id '" + id + "'. Registered: " + byId.keySet());
        }
        return s;
    }

    public Set<String> ids() {
        return byId.keySet();
    }
}
