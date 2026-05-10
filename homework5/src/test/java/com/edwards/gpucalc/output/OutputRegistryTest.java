package com.edwards.gpucalc.output;

import com.edwards.gpucalc.app.GpuCalcApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class OutputRegistryTest {

    @Autowired
    OutputRegistry registry;

    @Test
    void pngSinkIsRegistered() {
        assertThat(registry.ids()).contains("png");
        assertThat(registry.find("png")).isPresent();
    }

    @Test
    void requireReturnsSameBeanAsFind() {
        OutputSink byFind = registry.find("png").orElseThrow();
        OutputSink byRequire = registry.require("png");
        assertThat(byRequire).isSameAs(byFind);
    }

    @Test
    void findUnknownReturnsEmpty() {
        assertThat(registry.find("nope")).isEmpty();
    }

    @Test
    void requireUnknownThrows() {
        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
