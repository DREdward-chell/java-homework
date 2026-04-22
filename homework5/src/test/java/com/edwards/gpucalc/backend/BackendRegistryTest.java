package com.edwards.gpucalc.backend;

import com.edwards.gpucalc.app.GpuCalcApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class BackendRegistryTest {

    @Autowired
    BackendRegistry registry;

    @Test
    void javaBackendIsRegistered() {
        assertThat(registry.ids()).contains("java");
        assertThat(registry.find("java")).isPresent();
    }

    @Test
    void requireReturnsSameBeanAsFind() {
        RenderBackend byFind = registry.find("java").orElseThrow();
        RenderBackend byRequire = registry.require("java");
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
