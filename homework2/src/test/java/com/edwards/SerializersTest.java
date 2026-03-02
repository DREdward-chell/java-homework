package com.edwards;

import com.edwards.collections.DynamicArray;
import com.edwards.collections.serializers.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SerializersTest {
    @Test
    public void serialize() {
        DynamicArray<Object> array = DynamicArray.of(1, "e", 4, "242");
        var xml = new XmlDynamicArraySerializer();
        var csv = new CsvDynamicArraySerializer();
        var json = new JsonDynamicArraySerializer();
        var bin = new BinaryDynamicArraySerializer();

        System.out.println(csv.serialize(array));
        System.out.println("---");
        System.out.println(bin.serialize(array));
        System.out.println("---");
        System.out.println(json.serialize(array));
        System.out.println("---");
        System.out.println(xml.serialize(array));
    }

    @Test
    public void deserialize() {
        DynamicArray<Object> array = DynamicArray.of(1, "e", 4, "242");
        var xml = new XmlDynamicArraySerializer();
        var csv = new CsvDynamicArraySerializer();
        var json = new JsonDynamicArraySerializer();
        var bin = new BinaryDynamicArraySerializer();

        var serialized_xml = xml.serialize(array);
        var serialized_json = json.serialize(array);
        var serialized_csv = csv.serialize(array);
        var serialized_bin = bin.serialize(array);

        var deserialized_xml = xml.deserialize(serialized_xml);
        var deserialized_json = json.deserialize(serialized_json);
        var deserialized_csv = csv.deserialize(serialized_csv);
        var deserialized_bin = bin.deserialize(serialized_bin);

        assertArrayEquals(deserialized_xml.toArray(), deserialized_json.toArray());
        assertArrayEquals(deserialized_bin.toArray(), deserialized_csv.toArray());
        assertArrayEquals(deserialized_bin.toArray(), deserialized_xml.toArray());
        assertArrayEquals(deserialized_xml.toArray(), array.toArray());
    }
}
