package com.edwards.csvparser;

import java.util.Objects;
import java.util.Set;

final class Converters {

    private static final Set<Class<?>> SCALAR_TYPES = Set.of(
            String.class,
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            boolean.class, Boolean.class
    );

    private Converters() {}

    static boolean isScalar(Class<?> type) {
        return SCALAR_TYPES.contains(type);
    }

    static Object fromString(String raw, Class<?> type) {
        if (type == String.class) {
            return raw;
        }
        if (raw.isEmpty()) {
            if (type.isPrimitive()) {
                throw new CsvParserException("Empty cell cannot be assigned to primitive " + type.getName());
            }
            return null;
        }
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(raw);
            if (type == long.class || type == Long.class) return Long.parseLong(raw);
            if (type == double.class || type == Double.class) return Double.parseDouble(raw);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(raw);
        } catch (NumberFormatException e) {
            throw new CsvParserException("Cannot parse '" + raw + "' as " + type.getSimpleName(), e);
        }
        throw new CsvParserException("Unsupported scalar type: " + type.getName());
    }

    static String toString(Object value) {
        return Objects.requireNonNullElse(value, "").toString();
    }
}
