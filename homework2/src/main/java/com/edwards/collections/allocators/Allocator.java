package com.edwards.collections.allocators;

public interface Allocator<T> {
    T[] allocateNew(int capacity);
    T[] reallocate(T[] array, int newCapacity);
}
