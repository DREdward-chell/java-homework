package com.edwards.gpucalc.backend.opengl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShaderSourceBuilderTest {

    private final ShaderSourceBuilder builder = new ShaderSourceBuilder();

    @Test
    void buildsCompleteShaderWithRequestedVariations() {
        String src = builder.build("/shaders/compute/chaos_game.comp", List.of("linear", "swirl"));
        assertThat(src).contains("#version 430 core");
        assertThat(src).contains("vec2 variation_linear");
        assertThat(src).contains("vec2 variation_swirl");
        assertThat(src).doesNotContain(ShaderSourceBuilder.INCLUDE_SENTINEL);
    }

    @Test
    void deduplicatesVariationIds() {
        String src = builder.build("/shaders/compute/chaos_game.comp",
                List.of("swirl", "swirl", "linear"));
        int occurrences = src.split("vec2 variation_swirl\\(", -1).length - 1;
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    void rejectsTemplateMissingSentinel() {
        ShaderSourceBuilder b = new ShaderSourceBuilder();
        assertThatThrownBy(() -> b.build("/variations/linear.inc", List.of("linear")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinel");
    }

    @Test
    void rejectsUnknownVariationFile() {
        assertThatThrownBy(() -> builder.build("/shaders/compute/chaos_game.comp",
                List.of("nonexistent-variation")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonexistent-variation");
    }

    @Test
    void allFourInitialVariationsResolve() {
        String src = builder.build("/shaders/compute/chaos_game.comp",
                List.of("linear", "sinusoidal", "spherical", "swirl"));
        assertThat(src).contains("variation_linear");
        assertThat(src).contains("variation_sinusoidal");
        assertThat(src).contains("variation_spherical");
        assertThat(src).contains("variation_swirl");
    }
}
