package com.edwards.gpucalc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpochResolutionTest {

    @Test
    void epochCountDerivesStrideAsIterCountOverCount() {
        CliOverrides cli = new CliOverrides(null, null, null, 10_000,
                null, null, null, "linear:1.0", 10, null, null);
        ResolvedConfig r = resolver().resolve(cli, null);
        assertThat(r.epochs()).isNotNull();
        assertThat(r.epochs().epochCount()).isEqualTo(10);
        assertThat(r.epochs().epochStride()).isEqualTo(1_000);
    }

    @Test
    void epochStrideDerivesCountAsIterCountOverStride() {
        CliOverrides cli = new CliOverrides(null, null, null, 10_000,
                null, null, null, "linear:1.0", null, 2_500, null);
        ResolvedConfig r = resolver().resolve(cli, null);
        assertThat(r.epochs()).isNotNull();
        assertThat(r.epochs().epochStride()).isEqualTo(2_500);
        assertThat(r.epochs().epochCount()).isEqualTo(4);
    }

    @Test
    void providingBothCountAndStrideIsRejected() {
        CliOverrides cli = new CliOverrides(null, null, null, 10_000,
                null, null, null, "linear:1.0", 10, 1_000, null);
        ConfigValidationException ex = catchValidation(() -> resolver().resolve(cli, null));
        assertThat(ex.getErrors()).anyMatch(s -> s.contains("mutually exclusive"));
    }

    private static ConfigValidationException catchValidation(Runnable r) {
        try {
            r.run();
            throw new AssertionError("expected ConfigValidationException");
        } catch (ConfigValidationException e) {
            return e;
        }
    }

    @Test
    void epochsOmittedWhenNeitherSet() {
        CliOverrides cli = new CliOverrides(null, null, null, 10_000,
                null, null, null, "linear:1.0", null, null, null);
        ResolvedConfig r = resolver().resolve(cli, null);
        assertThat(r.epochs()).isNull();
    }

    @Test
    void gifDelayFallsBackToDefault() {
        CliOverrides cli = new CliOverrides(null, null, null, null,
                null, null, null, "linear:1.0", null, null, null);
        ResolvedConfig r = resolver().resolve(cli, null);
        assertThat(r.gifDelayCentiseconds()).isEqualTo(ResolvedConfig.DEFAULT_GIF_DELAY_CS);
    }

    @Test
    void cliGifDelayWinsOverDefault() {
        CliOverrides cli = new CliOverrides(null, null, null, null,
                null, null, null, "linear:1.0", null, null, 50);
        ResolvedConfig r = resolver().resolve(cli, null);
        assertThat(r.gifDelayCentiseconds()).isEqualTo(50);
    }

    @Test
    void epochConfigRejectsNonPositive() {
        assertThatThrownBy(() -> new EpochConfig(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EpochConfig(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawConfigEpochFieldsFlowThrough() {
        RawConfig raw = new RawConfig();
        raw.epochCount = 5;
        raw.gifDelayCentiseconds = 25;
        raw.iterationCount = 5_000;
        CliOverrides cli = new CliOverrides(null, null, null, null,
                null, null, null, "linear:1.0", null, null, null);
        ResolvedConfig r = resolver().resolve(cli, raw);
        assertThat(r.epochs()).isNotNull();
        assertThat(r.epochs().epochCount()).isEqualTo(5);
        assertThat(r.epochs().epochStride()).isEqualTo(1_000);
        assertThat(r.gifDelayCentiseconds()).isEqualTo(25);
    }

    private static ConfigResolver resolver() {
        ConfigValidator validator = new ConfigValidator(new VariationCatalog());
        return new ConfigResolver(new AffineParamsParser(), new FunctionsParser(), validator);
    }
}
