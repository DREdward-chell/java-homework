package com.edwards.gpucalc.backend.opencl;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class DeviceSelector {

    private DeviceSelector() {
    }

    public static int resolvePlatform(@Nullable String selector, List<String> platformNames) {
        if (platformNames.isEmpty()) {
            throw new IllegalArgumentException("no OpenCL platforms available");
        }
        if (selector == null || selector.isBlank()) return 0;
        Integer byIndex = tryParseIndex(selector, platformNames.size());
        if (byIndex != null) return byIndex;
        int bySubstr = findFirstSubstring(selector, platformNames);
        if (bySubstr >= 0) return bySubstr;
        throw new IllegalArgumentException(
                "--cl-platform '" + selector + "' matched no platform. Available: " + platformNames);
    }

    public static int resolveDevice(@Nullable String selector,
                                    List<String> deviceNames,
                                    List<Boolean> isGpu) {
        if (deviceNames.isEmpty()) {
            throw new IllegalArgumentException("no OpenCL devices on the selected platform");
        }
        if (deviceNames.size() != isGpu.size()) {
            throw new IllegalArgumentException(
                    "deviceNames/isGpu length mismatch (" + deviceNames.size()
                            + " vs " + isGpu.size() + ")");
        }
        if (selector == null || selector.isBlank()) {
            for (int i = 0; i < isGpu.size(); i++) {
                if (Boolean.TRUE.equals(isGpu.get(i))) return i;
            }
            return 0;
        }
        Integer byIndex = tryParseIndex(selector, deviceNames.size());
        if (byIndex != null) return byIndex;
        int bySubstr = findFirstSubstring(selector, deviceNames);
        if (bySubstr >= 0) return bySubstr;
        throw new IllegalArgumentException(
                "--cl-device '" + selector + "' matched no device. Available: " + deviceNames);
    }

    @Nullable
    private static Integer tryParseIndex(String s, int listSize) {
        try {
            int i = Integer.parseInt(s.trim());
            if (i < 0 || i >= listSize) return null;
            return i;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int findFirstSubstring(String needle, List<String> haystack) {
        String n = needle.toLowerCase();
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).toLowerCase().contains(n)) return i;
        }
        return -1;
    }
}
