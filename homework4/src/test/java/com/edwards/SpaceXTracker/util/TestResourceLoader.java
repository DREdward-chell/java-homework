package com.edwards.SpaceXTracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestResourceLoader {

    private TestResourceLoader() {}

    public static String loadResource(String filename) {
        try (InputStream is = TestResourceLoader.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + filename, e);
        }
    }
}
