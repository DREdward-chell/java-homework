package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AffineParamsParserTest {

    private final AffineParamsParser parser = new AffineParamsParser();

    @Test
    void parsesSingleTransform() {
        List<AffineCoeffs> out = parser.parse("1,2,3,4,5,6");
        assertThat(out).containsExactly(new AffineCoeffs(1, 2, 3, 4, 5, 6));
    }

    @Test
    void parsesMultipleTransforms() {
        List<AffineCoeffs> out = parser.parse("0.5,0,0,0,0.5,0/0.5,0,0.5,0,0.5,0.5/1,0,0,0,1,0");
        assertThat(out).containsExactly(
                new AffineCoeffs(0.5, 0, 0, 0, 0.5, 0),
                new AffineCoeffs(0.5, 0, 0.5, 0, 0.5, 0.5),
                new AffineCoeffs(1, 0, 0, 0, 1, 0));
    }

    @Test
    void toleratesWhitespace() {
        List<AffineCoeffs> out = parser.parse(" 1, 2 , 3,4,5,6 / 7,8,9,10,11,12 ");
        assertThat(out).hasSize(2);
        assertThat(out.get(1)).isEqualTo(new AffineCoeffs(7, 8, 9, 10, 11, 12));
    }

    @Test
    void parsesNegativeAndFractional() {
        List<AffineCoeffs> out = parser.parse("-0.5,1e-3,-2.5,.25,1E2,0");
        assertThat(out).containsExactly(new AffineCoeffs(-0.5, 1e-3, -2.5, 0.25, 100.0, 0.0));
    }

    @Test
    void rejectsEmptyString() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsWhitespaceOnly() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(ConfigParseException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("null");
    }

    @Test
    void rejectsWrongCoefficientCount() {
        assertThatThrownBy(() -> parser.parse("1,2,3,4,5"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("6");
    }

    @Test
    void rejectsTooManyCoefficients() {
        assertThatThrownBy(() -> parser.parse("1,2,3,4,5,6,7"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("6");
    }

    @Test
    void rejectsNonNumericCoefficient() {
        assertThatThrownBy(() -> parser.parse("1,2,abc,4,5,6"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("abc");
    }

    @Test
    void rejectsEmptyTransformInSequence() {
        assertThatThrownBy(() -> parser.parse("1,2,3,4,5,6//7,8,9,10,11,12"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void errorMessageIdentifiesTransformNumber() {
        assertThatThrownBy(() -> parser.parse("1,2,3,4,5,6/not,a,valid,one,at,all"))
                .isInstanceOf(ConfigParseException.class)
                .hasMessageContaining("transform #2");
    }
}
