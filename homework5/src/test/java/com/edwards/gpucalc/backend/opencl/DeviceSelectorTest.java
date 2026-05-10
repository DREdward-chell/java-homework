package com.edwards.gpucalc.backend.opencl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceSelectorTest {

    private static final List<String> PLATFORMS = List.of(
            "NVIDIA CUDA", "Intel(R) OpenCL", "Apple");
    private static final List<String> DEVICES = List.of(
            "GeForce RTX 4090", "Intel UHD Graphics", "Intel Xeon W-2295");
    private static final List<Boolean> IS_GPU = List.of(true, true, false);

    @Test
    void nullPlatformSelectorPicksIndexZero() {
        assertThat(DeviceSelector.resolvePlatform(null, PLATFORMS)).isEqualTo(0);
    }

    @Test
    void blankPlatformSelectorPicksIndexZero() {
        assertThat(DeviceSelector.resolvePlatform("  ", PLATFORMS)).isEqualTo(0);
    }

    @Test
    void integerPlatformSelectorIsUsedWhenInRange() {
        assertThat(DeviceSelector.resolvePlatform("1", PLATFORMS)).isEqualTo(1);
        assertThat(DeviceSelector.resolvePlatform("2", PLATFORMS)).isEqualTo(2);
    }

    @Test
    void substringPlatformSelectorIsCaseInsensitive() {
        assertThat(DeviceSelector.resolvePlatform("intel", PLATFORMS)).isEqualTo(1);
        assertThat(DeviceSelector.resolvePlatform("NVIDIA", PLATFORMS)).isEqualTo(0);
        assertThat(DeviceSelector.resolvePlatform("apple", PLATFORMS)).isEqualTo(2);
    }

    @Test
    void outOfRangeIndexFallsThroughToSubstringMatchThenThrows() {
        assertThatThrownBy(() -> DeviceSelector.resolvePlatform("99", PLATFORMS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matched no platform");
        assertThatThrownBy(() -> DeviceSelector.resolvePlatform("nonesuch", PLATFORMS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyPlatformListRejects() {
        assertThatThrownBy(() -> DeviceSelector.resolvePlatform(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no OpenCL platforms");
    }

    @Test
    void nullDeviceSelectorPrefersFirstGpu() {
        assertThat(DeviceSelector.resolveDevice(null, DEVICES, IS_GPU)).isEqualTo(0);
    }

    @Test
    void nullDeviceSelectorFallsBackToIndexZeroWhenNoGpu() {
        List<String> cpuOnly = List.of("CPU-A", "CPU-B");
        List<Boolean> none = List.of(false, false);
        assertThat(DeviceSelector.resolveDevice(null, cpuOnly, none)).isEqualTo(0);
    }

    @Test
    void deviceIndexSelectorMatchesGivenPosition() {
        assertThat(DeviceSelector.resolveDevice("2", DEVICES, IS_GPU)).isEqualTo(2);
    }

    @Test
    void deviceSubstringSelectorMatchesCaseInsensitively() {
        assertThat(DeviceSelector.resolveDevice("uhd", DEVICES, IS_GPU)).isEqualTo(1);
    }

    @Test
    void lengthMismatchBetweenDevicesAndIsGpuRejects() {
        assertThatThrownBy(() -> DeviceSelector.resolveDevice(null,
                List.of("a", "b"), List.of(true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length mismatch");
    }
}
