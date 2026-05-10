package com.edwards.gpucalc.backend.opencl;

import com.edwards.gpucalc.app.GpuCalcApplication;
import com.edwards.gpucalc.backend.BackendRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GpuCalcApplication.class)
class OpenClBackendRegistryTest {

    @Autowired
    BackendRegistry registry;

    @Test
    void openClBeanRegistrationTracksAvailability() {
        boolean available = ClAvailable.available();
        if (available) {
            assertThat(registry.ids()).contains("opencl");
        } else {
            assertThat(registry.ids()).doesNotContain("opencl");
        }
    }
}
