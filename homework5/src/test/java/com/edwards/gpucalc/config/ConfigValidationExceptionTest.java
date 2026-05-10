package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigValidationExceptionTest {

    @Test
    void carriesErrorList() {
        ConfigValidationException e = new ConfigValidationException(List.of("a", "b"));
        assertThat(e.getErrors()).containsExactly("a", "b");
    }

    @Test
    void summaryMentionsErrorCount() {
        ConfigValidationException e = new ConfigValidationException(List.of("a", "b", "c"));
        assertThat(e.getMessage()).contains("3");
    }

    @Test
    void bulletedProducesDashedLines() {
        ConfigValidationException e = new ConfigValidationException(List.of("first", "second"));
        assertThat(e.bulleted())
                .contains("  - first")
                .contains("  - second");
    }

    @Test
    void errorListIsImmutable() {
        List<String> source = new java.util.ArrayList<>(List.of("a"));
        ConfigValidationException e = new ConfigValidationException(source);
        source.add("b");
        assertThat(e.getErrors()).containsExactly("a");
    }
}
