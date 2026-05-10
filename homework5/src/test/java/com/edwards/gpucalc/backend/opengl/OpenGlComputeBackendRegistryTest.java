package com.edwards.gpucalc.backend.opengl;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.BackendRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GpuCalcApplication.class)
class OpenGlComputeBackendRegistryTest {

    @Autowired
    BackendRegistry registry;

    @Test
    @EnabledOnOs(OS.MAC)
    void openglComputeNotRegisteredOnMac() {
        assertThat(registry.ids()).doesNotContain("opengl-compute");
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void requireEmitsPlatformAwareMessageOnMac() {
        assertThatThrownBy(() -> registry.require("opengl-compute"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported on macOS")
                .hasMessageContaining("java")
                .hasMessageContaining("opencl");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.WINDOWS})
    void openglComputeRegisteredOnSupportedPlatforms() {
        assertThat(registry.ids()).contains("opengl-compute");
    }
}
