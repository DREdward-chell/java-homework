package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigValidatorTest {

    private final VariationCatalog catalog = new VariationCatalog();
    private final ConfigValidator validator = new ConfigValidator(catalog);

    private static final List<AffineCoeffs> OK_AFFINES = List.of(new AffineCoeffs(1, 0, 0, 0, 1, 0));
    private static final List<WeightedVariationRef> OK_FUNCS = List.of(new WeightedVariationRef("swirl", 1.0));

    private static ResolvedConfig validBase() {
        return new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 1, OK_AFFINES, OK_FUNCS);
    }

    @Test
    void validConfigProducesNoErrors() {
        assertThat(validator.validate(validBase())).isEmpty();
    }

    @Test
    void rejectsNonPositiveWidth() {
        ResolvedConfig c = new ResolvedConfig(0, 1080, 5.0, 2500, "result.png", 1, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("width"));
    }

    @Test
    void rejectsNonPositiveHeight() {
        ResolvedConfig c = new ResolvedConfig(1920, -1, 5.0, 2500, "result.png", 1, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("height"));
    }

    @Test
    void rejectsZeroThreads() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 0, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("threads"));
    }

    @Test
    void rejectsZeroIterations() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 0, "result.png", 1, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("iteration"));
    }

    @Test
    void rejectsBlankOutputPath() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "   ", 1, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("output-path"));
    }

    @Test
    void rejectsEmptyAffineParams() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 1, List.of(), OK_FUNCS);
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("affine"));
    }

    @Test
    void rejectsEmptyFunctions() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 1, OK_AFFINES, List.of());
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("variation"));
    }

    @Test
    void rejectsNonPositiveWeight() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 1,
                OK_AFFINES, List.of(new WeightedVariationRef("swirl", 0.0)));
        assertThat(validator.validate(c)).anyMatch(m -> m.contains("weight"));
    }

    @Test
    void rejectsUnknownVariationName() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, 5.0, 2500, "result.png", 1,
                OK_AFFINES, List.of(new WeightedVariationRef("banana", 1.0)));
        assertThat(validator.validate(c))
                .anyMatch(m -> m.contains("banana") && m.contains("unknown"));
    }

    @Test
    void aggregatesMultipleErrors() {
        ResolvedConfig c = new ResolvedConfig(-1, -1, 5.0, 0, " ", 0, List.of(),
                List.of(new WeightedVariationRef("banana", -1.0)));
        List<String> errs = validator.validate(c);
        assertThat(errs.size()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void acceptsEverySeedValueIncludingNegativeAndFractional() {
        ResolvedConfig c = new ResolvedConfig(1920, 1080, -3.14, 2500, "r.png", 1, OK_AFFINES, OK_FUNCS);
        assertThat(validator.validate(c)).isEmpty();
    }
}
