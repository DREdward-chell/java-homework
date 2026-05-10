package com.edwards.gpucalc.config;

import com.edwards.gpucalc.core.AffineCoeffs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AffineParamsParser {

    private static final int COEFFS_PER_TRANSFORM = 6;

    public List<AffineCoeffs> parse(String spec) {
        if (spec == null) {
            throw new ConfigParseException("--affine-params: spec is null");
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            throw new ConfigParseException("--affine-params: spec is empty");
        }

        String[] parts = trimmed.split("/");
        List<AffineCoeffs> result = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            result.add(parseOne(parts[i], i));
        }
        return result;
    }

    private AffineCoeffs parseOne(String part, int index) {
        String trimmed = part.trim();
        if (trimmed.isEmpty()) {
            throw new ConfigParseException(
                    "--affine-params: transform #" + (index + 1) + " is empty");
        }
        String[] tokens = trimmed.split(",");
        if (tokens.length != COEFFS_PER_TRANSFORM) {
            throw new ConfigParseException(
                    "--affine-params: transform #" + (index + 1)
                            + " must have exactly " + COEFFS_PER_TRANSFORM
                            + " comma-separated doubles (got " + tokens.length + ")");
        }
        double[] coeffs = new double[COEFFS_PER_TRANSFORM];
        for (int j = 0; j < COEFFS_PER_TRANSFORM; j++) {
            coeffs[j] = parseDouble(tokens[j], index, j);
        }
        return new AffineCoeffs(coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4], coeffs[5]);
    }

    private double parseDouble(String token, int transformIndex, int coeffIndex) {
        String trimmed = token.trim();
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            throw new ConfigParseException(
                    "--affine-params: transform #" + (transformIndex + 1)
                            + ", coefficient #" + (coeffIndex + 1)
                            + " is not a valid double: '" + trimmed + "'", e);
        }
    }
}
