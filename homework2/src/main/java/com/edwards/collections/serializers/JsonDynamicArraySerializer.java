package com.edwards.collections.serializers;

import com.edwards.collections.CVector;
import com.edwards.collections.DynamicArray;

public class JsonDynamicArraySerializer implements DynamicArraySerializer {

    @Override
    public String serialize(DynamicArray<?> array) {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJsonValue(array.get(i)));
        }

        json.append("]");
        return json.toString();
    }

    private String toJsonValue(Object obj) {
        return switch (obj) {
            case null -> "null";
            case String o -> "\"" + escapeJson(o) + "\"";
            case Number x -> x.toString();
            case Boolean x -> x.toString();
            default -> "\"" + obj + "\"";
        };
    }

    private String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public DynamicArray<?> deserialize(String data) {
        DynamicArray<Object> vector = new CVector<>();

        if (data == null) {
            return vector;
        }

        String trimmed = data.trim();

        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return vector;
        }

        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array format");
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return vector;
        }

        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (c == ',' && !inString) {
                addElement(vector, current.toString().trim());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            addElement(vector, current.toString().trim());
        }

        return vector;
    }

    private void addElement(DynamicArray<Object> vector, String value) {
        if (value.equals("null")) {
            vector.add(null);
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            String unescaped = unescapeJson(value.substring(1, value.length() - 1));
            vector.add(unescaped);
        } else if (value.equals("true") || value.equals("false")) {
            vector.add(Boolean.parseBoolean(value));
        } else {
            try {
                if (value.contains(".")) {
                    vector.add(Double.parseDouble(value));
                } else {
                    vector.add(Integer.parseInt(value));
                }
            } catch (NumberFormatException e) {
                vector.add(value);
            }
        }
    }

    private String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                switch (c) {
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}