package com.edwards.collections.serializers;

import com.edwards.collections.CVector;
import com.edwards.collections.DynamicArray;

import java.util.Base64;

public class BinaryDynamicArraySerializer implements DynamicArraySerializer {
    private static final byte NULL_MARKER = 0x30; // '0' в ASCII
    private static final byte STRING_MARKER = 0x31; // '1' в ASCII
    private static final byte INTEGER_MARKER = 0x32; // '2' в ASCII
    private static final byte DOUBLE_MARKER = 0x33; // '3' в ASCII
    private static final byte BOOLEAN_MARKER = 0x34; // '4' в ASCII
    private static final byte BYTE_ARRAY_MARKER = 0x35; // '5' в ASCII
    private static final byte SEPARATOR = 0x3B; // ';' в ASCII

    @Override
    public String serialize(DynamicArray<?> array) {
        StringBuilder binary = new StringBuilder();

        for (int i = 0; i < array.size(); i++) {
            Object element = ((CVector<?>) array).get(i);

            switch (element) {
                case null -> {
                    binary.append((char) NULL_MARKER);
                    binary.append((char) SEPARATOR);
                    continue;
                }
                case String s -> {
                    binary.append((char) STRING_MARKER);
                    byte[] bytes = s.getBytes();
                    binary.append(Base64.getEncoder().encodeToString(bytes));
                }
                case Integer _ -> {
                    binary.append((char) INTEGER_MARKER);
                    binary.append(element);
                }
                case Long _ -> {
                    binary.append((char) INTEGER_MARKER);
                    binary.append('L').append(element);
                }
                case Double _ -> {
                    binary.append((char) DOUBLE_MARKER);
                    binary.append(element);
                }
                case Float _ -> {
                    binary.append((char) DOUBLE_MARKER);
                    binary.append('F').append(element);
                }
                case Boolean b -> {
                    binary.append((char) BOOLEAN_MARKER);
                    binary.append(b ? "true" : "false");
                }
                case byte[] bytes1 -> {
                    binary.append((char) BYTE_ARRAY_MARKER);
                    binary.append(Base64.getEncoder().encodeToString(bytes1));
                }
                default -> {
                    binary.append((char) STRING_MARKER);
                    byte[] bytes = element.toString().getBytes();
                    binary.append(Base64.getEncoder().encodeToString(bytes));
                }
            }

            binary.append((char) SEPARATOR);
        }

        return binary.toString();
    }

    @Override
    public DynamicArray<?> deserialize(String data) {
        DynamicArray<Object> vector = new CVector<>();

        if (data == null || data.isEmpty()) {
            return vector;
        }

        byte[] bytes = data.getBytes();
        int i = 0;

        while (i < bytes.length) {
            while (i < bytes.length && bytes[i] <= 0x20) {
                i++;
            }

            if (i >= bytes.length) break;

            byte marker = bytes[i++];

            StringBuilder valueBuilder = new StringBuilder();
            while (i < bytes.length && bytes[i] != SEPARATOR) {
                valueBuilder.append((char) bytes[i++]);
            }

            if (i < bytes.length && bytes[i] == SEPARATOR) {
                i++;
            }

            String valueStr = valueBuilder.toString();

            switch (marker) {
                case NULL_MARKER -> vector.add(null);

                case BOOLEAN_MARKER -> vector.add(Boolean.parseBoolean(valueStr));

                case STRING_MARKER -> {
                    try {
                        byte[] decodedBytes = Base64.getDecoder().decode(valueStr);
                        vector.add(new String(decodedBytes));
                    } catch (IllegalArgumentException e) {
                        vector.add(valueStr);
                    }
                }

                case INTEGER_MARKER -> {
                    try {
                        if (valueStr.startsWith("L")) {
                            vector.add(Long.parseLong(valueStr.substring(1)));
                        } else {
                            vector.add(Integer.parseInt(valueStr));
                        }
                    } catch (NumberFormatException e) {
                        vector.add(valueStr);
                    }
                }

                case DOUBLE_MARKER -> {
                    try {
                        if (valueStr.startsWith("F")) {
                            vector.add(Float.parseFloat(valueStr.substring(1)));
                        } else {
                            vector.add(Double.parseDouble(valueStr));
                        }
                    } catch (NumberFormatException e) {
                        vector.add(valueStr);
                    }
                }

                case BYTE_ARRAY_MARKER -> {
                    try {
                        byte[] decodedBytes = Base64.getDecoder().decode(valueStr);
                        vector.add(decodedBytes);
                    } catch (IllegalArgumentException e) {
                        vector.add(valueStr.getBytes());
                    }
                }

                default -> vector.add((char) marker + valueStr);
            }
        }

        return vector;
    }
}