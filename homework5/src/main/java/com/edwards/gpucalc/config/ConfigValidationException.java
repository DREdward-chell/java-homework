package com.edwards.gpucalc.config;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public final class ConfigValidationException extends RuntimeException {
    @Getter
    private final List<String> errors;

    public ConfigValidationException(List<String> errors) {
        super(buildSummary(errors));
        this.errors = List.copyOf(errors);
    }

    public String bulleted() {
        return errors.stream()
                .map(e -> "  - " + e)
                .collect(Collectors.joining("\n"));
    }

    private static String buildSummary(List<String> errors) {
        return "configuration validation failed with " + errors.size() + " error(s)";
    }
}
