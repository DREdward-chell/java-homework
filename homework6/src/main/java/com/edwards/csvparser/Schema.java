package com.edwards.csvparser;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Schema {

    private Schema() {}

    static List<FieldPath> resolve(Class<?> root) {
        List<FieldPath> paths = new ArrayList<>();
        collect(root, "", new ArrayList<>(), new LinkedHashSet<>(), paths);
        if (paths.isEmpty()) {
            throw new CsvParserException("Class " + root.getName() + " has no CSV-mappable fields");
        }
        return paths;
    }

    private static void collect(Class<?> type,
                                String prefix,
                                List<Field> chainSoFar,
                                Set<Class<?>> typeStack,
                                List<FieldPath> out) {
        if (!typeStack.add(type)) {
            throw new CsvParserException(
                    "Cyclic field reference detected through " + type.getName());
        }
        try {
            for (Field f : type.getDeclaredFields()) {
                if (shouldSkip(f)) continue;
                f.setAccessible(true);

                String header = prefix.isEmpty() ? headerName(f) : prefix + "." + headerName(f);
                List<Field> chain = append(chainSoFar, f);
                Class<?> ft = f.getType();

                if (Converters.isScalar(ft)) {
                    out.add(FieldPath.scalar(header, chain, ft));
                } else if (List.class.isAssignableFrom(ft)) {
                    out.add(FieldPath.list(header, chain, listElementType(f), listDelimiter(f)));
                } else {
                    collect(ft, header, chain, typeStack, out);
                }
            }
        } finally {
            typeStack.remove(type);
        }
    }

    private static boolean shouldSkip(Field f) {
        int m = f.getModifiers();
        return f.isSynthetic()
                || java.lang.reflect.Modifier.isStatic(m)
                || java.lang.reflect.Modifier.isTransient(m);
    }

    private static String headerName(Field f) {
        CsvName named = f.getAnnotation(CsvName.class);
        return named != null ? named.value() : f.getName();
    }

    private static String listDelimiter(Field f) {
        CsvCollection ann = f.getAnnotation(CsvCollection.class);
        return ann != null ? ann.delimiter() : CsvCollection.DEFAULT_DELIMITER;
    }

    private static Class<?> listElementType(Field f) {
        Type generic = f.getGenericType();
        if (generic instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> c && Converters.isScalar(c)) {
                return c;
            }
        }
        throw new CsvParserException(
                "Field " + f.getDeclaringClass().getSimpleName() + "." + f.getName()
                        + " must be List<T> with T in {String, Integer, Long, Double, Boolean}");
    }

    private static List<Field> append(List<Field> base, Field f) {
        List<Field> copy = new ArrayList<>(base.size() + 1);
        copy.addAll(base);
        copy.add(f);
        return copy;
    }
}
