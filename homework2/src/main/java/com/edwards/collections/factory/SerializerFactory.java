package com.edwards.collections.factory;

import com.edwards.collections.serializers.*;

public class SerializerFactory {
    public DynamicArraySerializer createSerializer(String format) {
        if (format.endsWith("json")) {
            return new JsonDynamicArraySerializer();
        }

        if (format.endsWith("xml")) {
            return new XmlDynamicArraySerializer();
        }

        if (format.endsWith("csv")) {
            return new CsvDynamicArraySerializer();
        }

        if (format.endsWith("bin")) {
            return new BinaryDynamicArraySerializer();
        }

        throw new IllegalArgumentException("Unknown format: " + format);
    }
}
