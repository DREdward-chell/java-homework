package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VariationCatalogTest {

    private final VariationCatalog catalog = new VariationCatalog();

    @Test
    void containsPhase2InitialVariations() {
        assertThat(catalog.contains("linear")).isTrue();
        assertThat(catalog.contains("sinusoidal")).isTrue();
        assertThat(catalog.contains("spherical")).isTrue();
        assertThat(catalog.contains("swirl")).isTrue();
    }

    @Test
    void containsCommonBonusVariations() {
        assertThat(catalog.contains("horseshoe")).isTrue();
        assertThat(catalog.contains("polar")).isTrue();
    }

    @Test
    void unknownNameNotContained() {
        assertThat(catalog.contains("banana")).isFalse();
    }

    @Test
    void namesReturnsNonEmptySet() {
        assertThat(catalog.names()).isNotEmpty();
    }
}
