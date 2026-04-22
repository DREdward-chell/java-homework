package com.edwards.gpucalc.core;

import com.edwards.gpucalc.app.GpuCalcApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class VariationRegistryTest {

    @Autowired
    VariationRegistry registry;

    @Test
    void containsAllFourPhase2Variations() {
        assertThat(registry.ids())
                .contains("linear", "sinusoidal", "spherical", "swirl");
    }

    @Test
    void findReturnsEmptyForUnknownName() {
        assertThat(registry.find("not-a-real-variation")).isEmpty();
    }

    @Test
    void findReturnsBeanForKnownName() {
        assertThat(registry.find("swirl")).isPresent();
    }

    @Test
    void requireThrowsForUnknown() {
        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sizeIsAtLeastFour() {
        assertThat(registry.size()).isGreaterThanOrEqualTo(4);
    }
}
