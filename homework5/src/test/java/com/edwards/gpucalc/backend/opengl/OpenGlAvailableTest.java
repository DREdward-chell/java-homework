package com.edwards.gpucalc.backend.opengl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenGlAvailableTest {

    @Test
    void reflectsCurrentPlatform() {
        boolean isMac = OpenGlAvailable.isMacOs();
        boolean expected = !isMac;
        assertThat(new OpenGlAvailable().matches(null, null)).isEqualTo(expected);
    }

    @Test
    void macPatternsRecognized() {
        String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertThat(OpenGlAvailable.isMacOs()).isTrue();
            System.setProperty("os.name", "darwin");
            assertThat(OpenGlAvailable.isMacOs()).isTrue();
            System.setProperty("os.name", "Linux");
            assertThat(OpenGlAvailable.isMacOs()).isFalse();
            System.setProperty("os.name", "Windows 11");
            assertThat(OpenGlAvailable.isMacOs()).isFalse();
        } finally {
            if (saved != null) System.setProperty("os.name", saved);
        }
    }
}
