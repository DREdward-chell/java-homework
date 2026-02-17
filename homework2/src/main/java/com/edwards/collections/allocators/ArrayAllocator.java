package com.edwards.collections.allocators;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ArrayAllocator<T> implements Allocator<T> {
    // private final Class<T> type;

    public ArrayAllocator(/*Class<T> type*/) {
        // this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] allocateNew(int capacity) {
        // return (T[]) Array.newInstance(type, capacity);
        return (T[]) new Object[capacity];
    }

    @Override
    public T[] reallocate(T[] array, int newCapacity) {
        return Arrays.copyOf(array, newCapacity);
    }
}