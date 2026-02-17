package com.edwards.collections.serializers;
import com.edwards.collections.DynamicArray;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DynamicArraySerializer {
    String serialize(DynamicArray<?> dynamicArray);
    DynamicArray<?> deserialize(String serialized);
}