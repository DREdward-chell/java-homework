package com.edwards.gpucalc.backend.opengl;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public final class OpenGlAvailable implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !isMacOs();
    }

    static boolean isMacOs() {
        String name = System.getProperty("os.name", "").toLowerCase();
        return name.contains("mac") || name.contains("darwin");
    }
}
