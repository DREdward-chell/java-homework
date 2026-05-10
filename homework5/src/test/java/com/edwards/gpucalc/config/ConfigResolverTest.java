package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigResolverTest {

    private final ConfigResolver resolver = new ConfigResolver(
            new AffineParamsParser(),
            new FunctionsParser(),
            new ConfigValidator(new VariationCatalog()));

    @Test
    void cliOnlyWithDefaultsForTheRest() {
        CliOverrides cli = new CliOverrides(
                null, null, 32.123531, 5000, null, 2, null, "swirl:1.0,horseshoe:0.8");
        ResolvedConfig cfg = resolver.resolve(cli, null);
        assertThat(cfg.width()).isEqualTo(Defaults.WIDTH);
        assertThat(cfg.height()).isEqualTo(Defaults.HEIGHT);
        assertThat(cfg.seed()).isEqualTo(32.123531);
        assertThat(cfg.iterationCount()).isEqualTo(5000);
        assertThat(cfg.outputPath()).isEqualTo(Defaults.OUTPUT_PATH);
        assertThat(cfg.threads()).isEqualTo(2);
        assertThat(cfg.affineParams()).isEqualTo(Defaults.AFFINE_PARAMS);
        assertThat(cfg.functions()).hasSize(2);
    }

    @Test
    void jsonOnlyUsesJsonValues() {
        RawConfig file = makeRawConfig();
        ResolvedConfig cfg = resolver.resolve(CliOverrides.empty(), file);
        assertThat(cfg.width()).isEqualTo(800);
        assertThat(cfg.height()).isEqualTo(600);
        assertThat(cfg.threads()).isEqualTo(4);
        assertThat(cfg.seed()).isEqualTo(2.5);
        assertThat(cfg.iterationCount()).isEqualTo(12345);
        assertThat(cfg.outputPath()).isEqualTo("out.png");
        assertThat(cfg.functions()).containsExactly(new WeightedVariationRef("swirl", 0.9));
        assertThat(cfg.affineParams()).containsExactly(new AffineCoeffs(1, 0, 0, 0, 1, 0));
    }

    @Test
    void cliOverridesJson() {
        RawConfig file = makeRawConfig();
        CliOverrides cli = new CliOverrides(
                null, null, null, null, null, 8, null, null);
        ResolvedConfig cfg = resolver.resolve(cli, file);
        assertThat(cfg.threads()).as("CLI threads wins").isEqualTo(8);
        assertThat(cfg.width()).as("JSON width still wins over default").isEqualTo(800);
    }

    @Test
    void cliFunctionsOverrideJsonFunctions() {
        RawConfig file = makeRawConfig();
        CliOverrides cli = new CliOverrides(
                null, null, null, null, null, null, null, "horseshoe:1.0");
        ResolvedConfig cfg = resolver.resolve(cli, file);
        assertThat(cfg.functions()).containsExactly(new WeightedVariationRef("horseshoe", 1.0));
    }

    @Test
    void cliAffinesOverrideJsonAffines() {
        RawConfig file = makeRawConfig();
        CliOverrides cli = new CliOverrides(
                null, null, null, null, null, null,
                "0.5,0,0,0,0.5,0/0.5,0,0.5,0,0.5,0", null);
        ResolvedConfig cfg = resolver.resolve(cli, file);
        assertThat(cfg.affineParams()).hasSize(2);
    }

    @Test
    void allDefaultsWhenCliAndJsonBothAbsentExceptFunctions() {
        CliOverrides cli = new CliOverrides(
                null, null, null, null, null, null, null, "swirl:1.0");
        ResolvedConfig cfg = resolver.resolve(cli, null);
        assertThat(cfg.width()).isEqualTo(Defaults.WIDTH);
        assertThat(cfg.height()).isEqualTo(Defaults.HEIGHT);
        assertThat(cfg.seed()).isEqualTo(Defaults.SEED);
        assertThat(cfg.iterationCount()).isEqualTo(Defaults.ITERATION_COUNT);
        assertThat(cfg.outputPath()).isEqualTo(Defaults.OUTPUT_PATH);
        assertThat(cfg.threads()).isEqualTo(Defaults.THREADS);
        assertThat(cfg.affineParams()).isEqualTo(Defaults.AFFINE_PARAMS);
    }

    @Test
    void parseErrorInCliSpecSurfacedAsValidationException() {
        CliOverrides cli = new CliOverrides(
                null, null, null, null, null, null, "1,2,3/4,5", null);
        assertThatThrownBy(() -> resolver.resolve(cli, null))
                .isInstanceOf(ConfigValidationException.class)
                .satisfies(e -> assertThat(((ConfigValidationException) e).getErrors())
                        .anyMatch(m -> m.contains("affine-params")));
    }

    @Test
    void invalidWidthProducesValidationException() {
        CliOverrides cli = new CliOverrides(
                -5, null, null, null, null, null, null, "swirl:1.0");
        assertThatThrownBy(() -> resolver.resolve(cli, null))
                .isInstanceOf(ConfigValidationException.class)
                .satisfies(e -> assertThat(((ConfigValidationException) e).getErrors())
                        .anyMatch(m -> m.contains("width")));
    }

    @Test
    void missingFunctionsSurfacesAsValidationError() {
        CliOverrides cli = CliOverrides.empty();
        assertThatThrownBy(() -> resolver.resolve(cli, null))
                .isInstanceOf(ConfigValidationException.class)
                .satisfies(e -> assertThat(((ConfigValidationException) e).getErrors())
                        .anyMatch(m -> m.contains("variation")));
    }

    @Test
    void jsonWithPartialAffineRowProducesValidationError() {
        RawConfig file = new RawConfig();
        RawConfig.RawAffine bad = new RawConfig.RawAffine();
        bad.a = 1.0; bad.b = 0.0; // c, d, e, f missing
        file.affineParams = List.of(bad);
        RawConfig.RawFunction ok = new RawConfig.RawFunction();
        ok.name = "swirl";
        ok.weight = 1.0;
        file.functions = List.of(ok);
        assertThatThrownBy(() -> resolver.resolve(CliOverrides.empty(), file))
                .isInstanceOf(ConfigValidationException.class)
                .satisfies(e -> assertThat(((ConfigValidationException) e).getErrors())
                        .anyMatch(m -> m.contains("affine_params")));
    }

    @Test
    void jsonWithPartialFunctionProducesValidationError() {
        RawConfig file = new RawConfig();
        RawConfig.RawFunction bad = new RawConfig.RawFunction();
        bad.name = "swirl"; // weight missing
        file.functions = List.of(bad);
        RawConfig.RawAffine okA = new RawConfig.RawAffine();
        okA.a = 1.0; okA.b = 0.0; okA.c = 0.0; okA.d = 0.0; okA.e = 1.0; okA.f = 0.0;
        file.affineParams = List.of(okA);
        assertThatThrownBy(() -> resolver.resolve(CliOverrides.empty(), file))
                .isInstanceOf(ConfigValidationException.class)
                .satisfies(e -> assertThat(((ConfigValidationException) e).getErrors())
                        .anyMatch(m -> m.contains("name") || m.contains("weight")));
    }

    @Test
    void sizeOnlyPartiallySetFallsBackForMissingDimension() {
        RawConfig file = new RawConfig();
        file.size = new RawConfig.Size();
        file.size.width = 640; // height null
        RawConfig.RawFunction fn = new RawConfig.RawFunction();
        fn.name = "swirl"; fn.weight = 1.0;
        file.functions = List.of(fn);
        ResolvedConfig cfg = resolver.resolve(CliOverrides.empty(), file);
        assertThat(cfg.width()).isEqualTo(640);
        assertThat(cfg.height()).isEqualTo(Defaults.HEIGHT);
    }

    private static RawConfig makeRawConfig() {
        RawConfig c = new RawConfig();
        c.size = new RawConfig.Size();
        c.size.width = 800;
        c.size.height = 600;
        c.threads = 4;
        c.seed = 2.5;
        c.iterationCount = 12345;
        c.outputPath = "out.png";
        RawConfig.RawFunction fn = new RawConfig.RawFunction();
        fn.name = "swirl";
        fn.weight = 0.9;
        c.functions = List.of(fn);
        RawConfig.RawAffine ap = new RawConfig.RawAffine();
        ap.a = 1.0; ap.b = 0.0; ap.c = 0.0; ap.d = 0.0; ap.e = 1.0; ap.f = 0.0;
        c.affineParams = List.of(ap);
        return c;
    }
}
