package com.edwards.gpucalc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class ConfigValidator {

    private final VariationCatalog variationCatalog;

    public List<String> validate(ResolvedConfig cfg) {
        List<String> errors = new ArrayList<>();

        if (cfg.width() <= 0) {
            errors.add("width must be > 0 (got " + cfg.width() + ")");
        }
        if (cfg.height() <= 0) {
            errors.add("height must be > 0 (got " + cfg.height() + ")");
        }
        if (cfg.threads() < 1) {
            errors.add("threads must be >= 1 (got " + cfg.threads() + ")");
        }
        if (cfg.iterationCount() < 1) {
            errors.add("iteration-count must be >= 1 (got " + cfg.iterationCount() + ")");
        }
        if (cfg.outputPath() == null || cfg.outputPath().isBlank()) {
            errors.add("output-path must be a non-blank string");
        }
        if (cfg.affineParams().isEmpty()) {
            errors.add("at least one affine transform must be specified");
        }
        if (cfg.functions().isEmpty()) {
            errors.add("at least one variation must be specified");
        }

        validateFunctions(cfg, errors);

        return errors;
    }

    private void validateFunctions(ResolvedConfig cfg, List<String> errors) {
        for (int i = 0; i < cfg.functions().size(); i++) {
            WeightedVariationRef f = cfg.functions().get(i);
            String label = "functions[" + i + "] '" + f.name() + "'";
            if (f.weight() <= 0) {
                errors.add(label + ": weight must be > 0 (got " + f.weight() + ")");
            }
            if (!variationCatalog.contains(f.name())) {
                errors.add(label + ": unknown variation. Known: "
                        + String.join(", ", new TreeSet<>(variationCatalog.names())));
            }
        }
    }
}
