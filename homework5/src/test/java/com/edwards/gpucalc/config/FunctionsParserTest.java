package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FunctionsParserTest {

    private final FunctionsParser parser = new FunctionsParser();

    @Test
    void parsesSingleEntry() {
        List<WeightedVariationRef> out = parser.parse("swirl:1.0");
        assertThat(out).containsExactly(new WeightedVariationRef("swirl", 1.0));
    }

    @Test
    void parsesMultipleEntries() {
        List<WeightedVariationRef> out = parser.parse("swirl:1.0,horseshoe:0.8,linear:0.5");
        assertThat(out).containsExactly(
                new WeightedVariationRef("swirl", 1.0),
                new WeightedVariationRef("horseshoe", 0.8),
                new WeightedVariationRef("linear", 0.5));
    }

    @Test
    void toleratesWhitespace() {
        List<WeightedVariationRef> out = parser.parse("  swirl : 1.0 , horseshoe : 0.8  ");
        assertThat(out).containsExactly(
                new WeightedVariationRef("swirl", 1.0),
                new WeightedVariationRef("horseshoe", 0.8));
    }

    @Test
    void acceptsZeroAndNegativeWeightsAtParseTime() {
        // The validator rejects non-positive weights; the parser just parses.
        List<WeightedVariationRef> out = parser.parse("swirl:0,horseshoe:-1.5");
        assertThat(out).containsExactly(
                new WeightedVariationRef("swirl", 0.0),
                new WeightedVariationRef("horseshoe", -1.5));
    }

    @Test
    void rejectsEmpty() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ConfigParseException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("null");
    }

    @Test
    void rejectsMissingWeight() {
        assertThatThrownBy(() -> parser.parse("swirl:"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("weight");
    }

    @Test
    void rejectsMissingColon() {
        assertThatThrownBy(() -> parser.parse("swirl"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining(":");
    }

    @Test
    void rejectsEmptyName() {
        assertThatThrownBy(() -> parser.parse(":1.0"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsNonNumericWeight() {
        assertThatThrownBy(() -> parser.parse("swirl:heavy"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("heavy");
    }

    @Test
    void rejectsEmptyEntryInSequence() {
        assertThatThrownBy(() -> parser.parse("swirl:1.0,,horseshoe:0.8"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void errorMessageIdentifiesEntryIndex() {
        assertThatThrownBy(() -> parser.parse("swirl:1.0,bad"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("#2");
    }
}
