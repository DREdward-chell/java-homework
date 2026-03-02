package com.edwards.collections.serializers;

import com.edwards.collections.CVector;
import com.edwards.collections.DynamicArray;

import java.util.Optional;

public class CsvDynamicArraySerializer implements DynamicArraySerializer {
    @Override
    public String serialize(DynamicArray<?> array) {
        StringBuilder csv = new StringBuilder();

        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                csv.append(",");
            }

            Object obj = array.get(i);

            switch (obj) {
                case String s -> csv.append("\"").append(escapeCsv(s)).append("\"");
                case Boolean _, Number _ -> csv.append(obj);
                default -> csv.append("\"").append(escapeCsv(obj.toString())).append("\"");
            }
        }

        return csv.toString();
    }

    private String escapeCsv(String str) {
        return str.replace("\"", "\"\"");
    }

    @Override
    public DynamicArray<?> deserialize(String data) {
        DynamicArray<Object> vector = new CVector<>();

        if (data == null || data.isEmpty()) {
            return vector;
        }

        SimpleCSVParser parser = new SimpleCSVParser(data);

        while (parser.hasNext()) {
            vector.add(parser.nextValue());
        }

        return vector;
    }

    private static class SimpleCSVParser {
        private final String data;
        private int position = 0;

        public SimpleCSVParser(String data) {
            this.data = data;
        }

        public boolean hasNext() {
            return position < data.length();
        }

        public Object nextValue() {
            while (position < data.length() && data.charAt(position) == ' ') {
                position++;
            }

            if (position >= data.length()) {
                return null;
            }

            char firstChar = data.charAt(position);

            if (firstChar == '"') {
                return parseQuotedString();
            }

            StringBuilder sb = new StringBuilder();
            while (position < data.length() && data.charAt(position) != ',') {
                sb.append(data.charAt(position));
                position++;
            }

            if (position < data.length() && data.charAt(position) == ',') {
                position++;
            }

            String value = sb.toString().trim();

            if (value.isEmpty()) {
                return null;
            }

            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                }
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                if (value.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                if (value.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                }
                return value;
            }
        }

        private String parseQuotedString() {
            StringBuilder sb = new StringBuilder();

            position++;

            while (position < data.length()) {
                char c = data.charAt(position);

                if (c == '"') {
                    if (position + 1 < data.length() && data.charAt(position + 1) == '"') {
                        sb.append('"');
                        position += 2;
                    } else {
                        position++;

                        if (position < data.length() && data.charAt(position) == ',') {
                            position++;
                        }

                        return sb.toString();
                    }
                } else {
                    sb.append(c);
                    position++;
                }
            }

            return sb.toString();
        }
    }
}