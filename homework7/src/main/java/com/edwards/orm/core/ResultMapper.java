package com.edwards.orm.core;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ResultMapper {
    private ResultMapper() {}

    public static Object map(Method method, List<Map<String, Object>> rows) {
        Class<?> raw = method.getReturnType();

        if (raw == List.class) {
            return mapMany(rows, elementType(method));
        }
        if (raw == Optional.class) {
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapOne(rows.get(0), elementType(method)));
        }
        if (raw == void.class || raw == Void.class) {
            return null;
        }
        if (raw == boolean.class || raw == Boolean.class) {
            return !rows.isEmpty();
        }
        if (rows.isEmpty()) {
            return null;
        }
        if (isScalar(raw)) {
            return scalar(rows.getFirst().values().iterator().next(), raw);
        }
        return mapOne(rows.getFirst(), raw);
    }

    public static Object mapUpdate(Method method, int affected) {
        Class<?> raw = method.getReturnType();
        if (raw == void.class || raw == Void.class)        return null;
        if (raw == int.class || raw == Integer.class)      return affected;
        if (raw == long.class || raw == Long.class)        return (long) affected;
        if (raw == boolean.class || raw == Boolean.class)  return affected > 0;
        throw new OrmException("Unsupported return type for non-SELECT: " + raw.getName());
    }

    private static Object mapOne(Map<String, Object> row, Class<?> type) {
        if (isScalar(type)) return scalar(row.values().iterator().next(), type);
        return EntityMetadata.of(type).fromRow(row);
    }

    private static List<Object> mapMany(List<Map<String, Object>> rows, Class<?> element) {
        if (isScalar(element)) {
            List<Object> out = new ArrayList<>(rows.size());
            for (Map<String, Object> r : rows) out.add(scalar(r.values().iterator().next(), element));
            return out;
        }
        EntityMetadata meta = EntityMetadata.of(element);
        List<Object> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) out.add(meta.fromRow(r));
        return out;
    }

    private static Class<?> elementType(Method method) {
        Type t = method.getGenericReturnType();
        if (t instanceof ParameterizedType pt && pt.getActualTypeArguments()[0] instanceof Class<?> c) {
            return c;
        }
        throw new OrmException("Cannot resolve generic element type of " + method);
    }

    private static boolean isScalar(Class<?> t) {
        return t == String.class
                || t == Long.class    || t == long.class
                || t == Integer.class || t == int.class
                || t == Double.class  || t == double.class
                || t == Boolean.class || t == boolean.class;
    }

    private static Object scalar(Object value, Class<?> target) {
        if (value == null) return null;
        if (target == long.class    || target == Long.class)    return ((Number) value).longValue();
        if (target == int.class     || target == Integer.class) return ((Number) value).intValue();
        if (target == double.class  || target == Double.class)  return ((Number) value).doubleValue();
        if (target == boolean.class || target == Boolean.class) return value instanceof Number n ? n.intValue() != 0 : value;
        return value;
    }
}
