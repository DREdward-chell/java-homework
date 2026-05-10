package com.edwards.csvparser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

final class FieldPath {

    enum Kind { SCALAR, LIST }

    private final String header;
    private final List<Field> chain;
    private final Kind kind;
    private final Class<?> elementType;
    private final String listDelimiter;

    private FieldPath(String header, List<Field> chain, Kind kind,
                      Class<?> elementType, String listDelimiter) {
        this.header = header;
        this.chain = chain;
        this.kind = kind;
        this.elementType = elementType;
        this.listDelimiter = listDelimiter;
    }

    static FieldPath scalar(String header, List<Field> chain, Class<?> type) {
        return new FieldPath(header, chain, Kind.SCALAR, type, null);
    }

    static FieldPath list(String header, List<Field> chain, Class<?> elementType, String delimiter) {
        return new FieldPath(header, chain, Kind.LIST, elementType, delimiter);
    }

    String header() {
        return header;
    }

    String read(Object root) {
        Object current = root;
        for (Field f : chain) {
            if (current == null) return "";
            current = getField(f, current);
        }
        return switch (kind) {
            case SCALAR -> Converters.toString(current);
            case LIST -> renderList(current);
        };
    }

    void write(Object root, String cell) {
        Object current = root;
        for (int i = 0; i < chain.size() - 1; i++) {
            Field f = chain.get(i);
            Object next = getField(f, current);
            if (next == null) {
                next = newInstance(f.getType());
                setField(f, current, next);
            }
            current = next;
        }
        Field leaf = chain.getLast();
        Object value = switch (kind) {
            case SCALAR -> Converters.fromString(cell, elementType);
            case LIST -> parseList(cell);
        };
        setField(leaf, current, value);
    }

    private String renderList(Object current) {
        if (current == null) return "";
        List<?> list = (List<?>) current;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(listDelimiter);
            sb.append(Converters.toString(list.get(i)));
        }
        return sb.toString();
    }

    private List<Object> parseList(String cell) {
        List<Object> result = new ArrayList<>();
        if (cell.isEmpty()) return result;
        // -1 разрешает пустые элементы: "a||b" -> ["a", "", "b"]
        for (String part : cell.split(java.util.regex.Pattern.quote(listDelimiter), -1)) {
            result.add(Converters.fromString(part, elementType));
        }
        return result;
    }

    private static Object getField(Field f, Object target) {
        try {
            return f.get(target);
        } catch (IllegalAccessException e) {
            throw new CsvParserException("Cannot read field " + f, e);
        }
    }

    private static void setField(Field f, Object target, Object value) {
        try {
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new CsvParserException("Cannot write field " + f, e);
        }
    }

    private static Object newInstance(Class<?> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new CsvParserException(
                    "Class " + type.getName() + " must have a no-arg constructor", e);
        }
    }
}
