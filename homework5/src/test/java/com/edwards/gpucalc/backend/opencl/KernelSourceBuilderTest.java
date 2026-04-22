package com.edwards.gpucalc.backend.opencl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KernelSourceBuilderTest {

    @Test
    void translatesVec2TypeToFloat2() {
        String src = "vec2 variation_foo(vec2 p) { return p; }";
        String out = KernelSourceBuilder.translateToCl(src);
        assertThat(out).isEqualTo("float2 variation_foo(float2 p) { return p; }");
    }

    @Test
    void translatesVec2ConstructorToFloat2Cast() {
        String src = "return vec2(1.0, 2.0);";
        String out = KernelSourceBuilder.translateToCl(src);
        assertThat(out).isEqualTo("return (float2)(1.0, 2.0);");
    }

    @Test
    void buildsKernelWithAllFiveVariationsAndRewritesTypes() {
        String out = new KernelSourceBuilder().build(
                "/kernels/chaos_game.cl",
                List.of("linear", "sinusoidal", "spherical", "swirl", "horseshoe"));
        // Sentinel replaced with variation bodies, translated into CL dialect.
        assertThat(out).doesNotContain("//#include \"variations\"");
        assertThat(out).contains("float2 variation_linear(float2 p)");
        assertThat(out).contains("float2 variation_sinusoidal(float2 p)");
        assertThat(out).contains("float2 variation_spherical(float2 p)");
        assertThat(out).contains("float2 variation_swirl(float2 p)");
        assertThat(out).contains("float2 variation_horseshoe(float2 p)");
        assertThat(out).contains("(float2)(0.0)");
        // The host kernel itself stays written in CL and is untouched by translation.
        assertThat(out).contains("__kernel void chaos_game");
        assertThat(out).contains("atomic_add(&hist[idx");
    }

    @Test
    void deduplicatesVariations() {
        String out = new KernelSourceBuilder().build(
                "/kernels/chaos_game.cl",
                List.of("linear", "linear", "linear"));
        // Should produce exactly one definition of variation_linear.
        int first = out.indexOf("float2 variation_linear(");
        int second = out.indexOf("float2 variation_linear(", first + 1);
        assertThat(first).isGreaterThan(-1);
        assertThat(second).isEqualTo(-1);
    }

    @Test
    void missingTemplateThrows() {
        assertThatThrownBy(() -> new KernelSourceBuilder()
                .build("/kernels/nonexistent.cl", List.of("linear")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("classpath resource not found");
    }

    @Test
    void missingVariationThrows() {
        assertThatThrownBy(() -> new KernelSourceBuilder()
                .build("/kernels/chaos_game.cl", List.of("no-such-variation")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-variation");
    }
}
