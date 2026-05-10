package com.edwards.gpucalc.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FunctionsParser {

    public List<WeightedVariationRef> parse(String spec) {
        if (spec == null) {
            throw new ConfigParseException("--functions: spec is null");
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            throw new ConfigParseException("--functions: spec is empty");
        }

        String[] entries = trimmed.split(",");
        List<WeightedVariationRef> result = new ArrayList<>(entries.length);
        for (int i = 0; i < entries.length; i++) {
            result.add(parseOne(entries[i], i));
        }
        return result;
    }

    private WeightedVariationRef parseOne(String entry, int index) {
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            throw new ConfigParseException(
                    "--functions: entry #" + (index + 1) + " is empty");
        }
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            throw new ConfigParseException(
                    "--functions: entry #" + (index + 1)
                            + " must be <name>:<weight> (got '" + trimmed + "')");
        }
        String name = trimmed.substring(0, colon).trim();
        String weightToken = trimmed.substring(colon + 1).trim();
        if (name.isEmpty()) {
            throw new ConfigParseException(
                    "--functions: entry #" + (index + 1) + " has empty name");
        }
        if (weightToken.isEmpty()) {
            throw new ConfigParseException(
                    "--functions: entry #" + (index + 1)
                            + " ('" + name + "') is missing weight");
        }
        double weight;
        try {
            weight = Double.parseDouble(weightToken);
        } catch (NumberFormatException e) {
            throw new ConfigParseException(
                    "--functions: entry #" + (index + 1)
                            + " ('" + name + "') weight is not a valid double: '"
                            + weightToken + "'", e);
        }
        return new WeightedVariationRef(name, weight);
    }
}
