package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfigResolver {

    private final AffineParamsParser affineParamsParser;
    private final FunctionsParser functionsParser;
    private final ConfigValidator validator;

    public ResolvedConfig resolve(CliOverrides cli, @Nullable RawConfig fileConfig) {
        List<String> errors = new ArrayList<>();

        int width = pickInt(cli.width(), sizeWidth(fileConfig), Defaults.WIDTH);
        int height = pickInt(cli.height(), sizeHeight(fileConfig), Defaults.HEIGHT);
        double seed = pickDouble(cli.seed(), fileConfig == null ? null : fileConfig.seed, Defaults.SEED);
        int iterationCount = pickInt(cli.iterationCount(),
                fileConfig == null ? null : fileConfig.iterationCount, Defaults.ITERATION_COUNT);
        String outputPath = pickString(cli.outputPath(),
                fileConfig == null ? null : fileConfig.outputPath, Defaults.OUTPUT_PATH);
        int threads = pickInt(cli.threads(),
                fileConfig == null ? null : fileConfig.threads, Defaults.THREADS);

        List<AffineCoeffs> affineParams = resolveAffines(cli, fileConfig, errors);
        List<WeightedVariationRef> functions = resolveFunctions(cli, fileConfig, errors);

        Integer rawEpochCount = pickNullable(cli.epochCount(),
                fileConfig == null ? null : fileConfig.epochCount);
        Integer rawEpochStride = pickNullable(cli.epochStride(),
                fileConfig == null ? null : fileConfig.epochStride);
        EpochConfig epochs = resolveEpochs(rawEpochCount, rawEpochStride, iterationCount, errors);
        int gifDelay = pickInt(cli.gifDelayCentiseconds(),
                fileConfig == null ? null : fileConfig.gifDelayCentiseconds,
                ResolvedConfig.DEFAULT_GIF_DELAY_CS);
        String clPlatform = pickNullableString(cli.clPlatform(),
                fileConfig == null ? null : fileConfig.clPlatform);
        String clDevice = pickNullableString(cli.clDevice(),
                fileConfig == null ? null : fileConfig.clDevice);

        ResolvedConfig candidate = new ResolvedConfig(
                width, height, seed, iterationCount, outputPath, threads, affineParams, functions,
                epochs, gifDelay, clPlatform, clDevice);

        errors.addAll(validator.validate(candidate));

        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
        return candidate;
    }

    private List<AffineCoeffs> resolveAffines(
            CliOverrides cli, @Nullable RawConfig fileConfig, List<String> errors) {
        if (cli.affineParams() != null) {
            try {
                return affineParamsParser.parse(cli.affineParams());
            } catch (ConfigParseException e) {
                errors.add(e.getMessage());
                return List.of();
            }
        }
        if (fileConfig != null && fileConfig.affineParams != null) {
            return fileConfig.affineParams.stream()
                    .map(ra -> toAffine(ra, errors))
                    .toList();
        }
        return Defaults.AFFINE_PARAMS;
    }

    private AffineCoeffs toAffine(RawConfig.RawAffine raw, List<String> errors) {
        if (raw.a == null || raw.b == null || raw.c == null
                || raw.d == null || raw.e == null || raw.f == null) {
            errors.add("affine_params: every entry requires all six fields a,b,c,d,e,f");
            return new AffineCoeffs(0, 0, 0, 0, 0, 0);
        }
        return new AffineCoeffs(raw.a, raw.b, raw.c, raw.d, raw.e, raw.f);
    }

    private List<WeightedVariationRef> resolveFunctions(
            CliOverrides cli, @Nullable RawConfig fileConfig, List<String> errors) {
        if (cli.functions() != null) {
            try {
                return functionsParser.parse(cli.functions());
            } catch (ConfigParseException e) {
                errors.add(e.getMessage());
                return List.of();
            }
        }
        if (fileConfig != null && fileConfig.functions != null) {
            return fileConfig.functions.stream()
                    .map(rf -> toFunction(rf, errors))
                    .toList();
        }
        return Defaults.FUNCTIONS;
    }

    private WeightedVariationRef toFunction(RawConfig.RawFunction raw, List<String> errors) {
        if (raw.name == null || raw.weight == null) {
            errors.add("functions: every entry requires both 'name' and 'weight'");
            return new WeightedVariationRef(raw.name == null ? "<missing>" : raw.name,
                    raw.weight == null ? 0.0 : raw.weight);
        }
        return new WeightedVariationRef(raw.name, raw.weight);
    }

    @Nullable
    private static Integer sizeWidth(@Nullable RawConfig cfg) {
        return cfg == null || cfg.size == null ? null : cfg.size.width;
    }

    @Nullable
    private static Integer sizeHeight(@Nullable RawConfig cfg) {
        return cfg == null || cfg.size == null ? null : cfg.size.height;
    }

    private static int pickInt(@Nullable Integer cli, @Nullable Integer file, int fallback) {
        if (cli != null) return cli;
        if (file != null) return file;
        return fallback;
    }

    private static double pickDouble(@Nullable Double cli, @Nullable Double file, double fallback) {
        if (cli != null) return cli;
        if (file != null) return file;
        return fallback;
    }

    private static String pickString(@Nullable String cli, @Nullable String file, String fallback) {
        if (cli != null) return cli;
        if (file != null) return file;
        return fallback;
    }

    @Nullable
    private static Integer pickNullable(@Nullable Integer cli, @Nullable Integer file) {
        if (cli != null) return cli;
        return file;
    }

    @Nullable
    private static String pickNullableString(@Nullable String cli, @Nullable String file) {
        if (cli != null) return cli;
        return file;
    }

    @Nullable
    private EpochConfig resolveEpochs(@Nullable Integer epochCount, @Nullable Integer epochStride,
                                      int iterationCount, List<String> errors) {
        if (epochCount == null && epochStride == null) return null;
        if (epochCount != null && epochStride != null) {
            errors.add("epoch-count and epoch-stride are mutually exclusive");
            return null;
        }
        if (iterationCount < 1) return null;
        if (epochCount != null) {
            if (epochCount < 1) {
                errors.add("epoch-count must be >= 1 (got " + epochCount + ")");
                return null;
            }
            int stride = Math.max(1, iterationCount / epochCount);
            return new EpochConfig(epochCount, stride);
        }
        if (epochStride < 1) {
            errors.add("epoch-stride must be >= 1 (got " + epochStride + ")");
            return null;
        }
        int count = Math.max(1, iterationCount / epochStride);
        return new EpochConfig(count, epochStride);
    }
}
