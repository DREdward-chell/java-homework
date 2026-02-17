package com.edwards.collections.factory;

import com.edwards.collections.serializers.*;

public class SerializerFactory {
    public DynamicArraySerializer createSerializer(String format) {
        var split = format.split("[.]");
        if (split.length < 2) {
            throw new IllegalArgumentException("Invalid file formating: " + format);
        }

        var f = split[split.length - 1];

        return switch (f) {
            case "xml" -> new XmlDynamicArraySerializer();
            case "json" -> new JsonDynamicArraySerializer();
            case "csv" -> new CsvDynamicArraySerializer();
            case "bin" -> new BinaryDynamicArraySerializer();
            default -> throw new IllegalArgumentException("Unknown format: " + f);
        };
    }
}
