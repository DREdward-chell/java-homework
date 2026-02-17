package com.edwards.collections.serializers;

import com.edwards.collections.CVector;
import com.edwards.collections.DynamicArray;

public class XmlDynamicArraySerializer implements DynamicArraySerializer {

    @Override
    public String serialize(DynamicArray<?> array) {
        CVector<?> vector = (CVector<?>) array;
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<array size=\"").append(vector.size()).append("\">\n");

        for (int i = 0; i < vector.size(); i++) {
            Object element = vector.get(i);
            xml.append("  <element index=\"").append(i).append("\" type=\"");
            xml.append(getTypeName(element));
            xml.append("\">");
            xml.append(escapeXml(element != null ? element.toString() : ""));
            xml.append("</element>\n");
        }

        xml.append("</array>");
        return xml.toString();
    }

    private String getTypeName(Object obj) {
        return switch (obj) {
            case null -> "null";
            case String _ -> "string";
            case Integer _ -> "integer";
            case Long _ -> "long";
            case Double _ -> "double";
            case Boolean _ -> "boolean";
            default -> "object";
        };
    }

    private String escapeXml(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public DynamicArray<?> deserialize(String data) {
        CVector<Object> vector = new CVector<>();

        String[] lines = data.split("\n");
        for (String s : lines) {
            String line = s.trim();
            if (line.startsWith("<element")) {
                String type = extractAttribute(line, "type");
                String content = extractContent(line);

                if ("null".equals(type)) {
                    vector.add(null);
                } else {
                    vector.add(parseByType(content, type));
                }
            }
        }

        return vector;
    }

    private String extractAttribute(String line, String attr) {
        String searchStr = attr + "=\"";
        int start = line.indexOf(searchStr) + searchStr.length();
        int end = line.indexOf("\"", start);
        return line.substring(start, end);
    }

    private String extractContent(String line) {
        int start = line.indexOf(">") + 1;
        int end = line.lastIndexOf("</");
        return unescapeXml(line.substring(start, end));
    }

    private String unescapeXml(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                int semicolon = text.indexOf(';', i);
                if (semicolon > i) {
                    String entity = text.substring(i, semicolon + 1);
                    switch (entity) {
                        case "&amp;":
                            sb.append('&');
                            break;
                        case "&lt;":
                            sb.append('<');
                            break;
                        case "&gt;":
                            sb.append('>');
                            break;
                        case "&quot;":
                            sb.append('"');
                            break;
                        case "&apos;":
                            sb.append('\'');
                            break;
                        default:
                            sb.append(entity);
                    }
                    i = semicolon;
                }
            } else {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    private Object parseByType(String value, String type) {
        return switch (type) {
            case "integer" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "double" -> Double.parseDouble(value);
            case "boolean" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}