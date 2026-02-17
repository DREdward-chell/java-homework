package com.edwards.collections;

import com.edwards.collections.allocators.Allocator;
import com.edwards.collections.allocators.ArrayAllocator;
import com.edwards.collections.capacitors.CapacityStrategy;
import com.edwards.collections.capacitors.DoublingStrategy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class CVector<T> implements DynamicArray<T> {
    CapacityStrategy capacityStrategy;
    Allocator<T> allocator;

    T[] data;
    int workingSize;

    public CVector() {
        capacityStrategy = new DoublingStrategy();
        allocator = new ArrayAllocator<>();
        workingSize = 0;
        data = allocator.allocateNew(1);
    }

    public CVector(int initialSize) {
        this();
        workingSize = initialSize;
        data = allocator.allocateNew(Math.clamp(initialSize, 1, Integer.MAX_VALUE));
    }


    public CVector(T[] data) {
        this();
        workingSize = data.length;
        this.data = Arrays.copyOf(data, Math.clamp(data.length, 1, Integer.MAX_VALUE));
    }

    private void growFor(int size) {
        int newCap = capacityStrategy.calculateNewCapacity(data.length, size);
        if (newCap > workingSize) {
            data = allocator.reallocate(data, newCap);
        }
    }

    @Override
    public int size() {
        return workingSize;
    }

    public int capacity() {
        return data.length;
    }

    @Override
    public boolean isEmpty() {
        return workingSize == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public T get(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= workingSize) {
            throw new IndexOutOfBoundsException();
        }
        return data[index];
    }

    @Override
    public T set(int index, T element) {
        if (index < 0 || index >= workingSize) {
            throw new IndexOutOfBoundsException();
        }
        T old = data[index];
        data[index] = element;
        return old;
    }

    @Override
    public T remove(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= workingSize) {
            throw new IndexOutOfBoundsException();
        }
        T old = data[index];
        System.arraycopy(data, index + 1, data, index, workingSize - index - 1);
        workingSize--;
        return old;
    }

    @Override
    public boolean remove(Object element) {
        int index = indexOf(element);
        if (index == -1) {
            return false;
        }
        remove(index);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            if (!contains(o)) {
                result |= remove(o);
            }
        }
        return result;
    }

    @Override
    public boolean add(T element) {
        growFor(workingSize + 1);
        data[workingSize] = element;
        workingSize++;
        return true;
    }

    @Override
    public boolean add(int index, T element) {
        growFor(workingSize + 1);
        workingSize++;
        System.arraycopy(data, index, data, index + 1, workingSize - index);
        data[index] = element;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (c.isEmpty()) {
            return false;
        }

        growFor(workingSize + c.size());
        for (T element : c) {
            add(element);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) throws IndexOutOfBoundsException {
        if (index < 0 || index > workingSize) {
            throw new IndexOutOfBoundsException();
        }

        if (c.isEmpty()) {
            return false;
        }

        if (index == workingSize) {
            return addAll(c);
        }

        growFor(workingSize + c.size());
        workingSize += c.size();

        System.arraycopy(data, index, data, index + c.size(), c.size() - 1);
        for (T element : c) {
            data[index++] = element;
        }
        return true;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < workingSize; i++) {
            if (data[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = workingSize - 1; i >= 0; i--) {
            if (data[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        workingSize = 0;
        data = allocator.allocateNew(1);
    }

    @Override
    public T[] toArray() {
        return Arrays.copyOf(data, workingSize);
    }

    @Deprecated
    @Override
    public <E> E[] toArray(E[] dst) {
        if (dst.length <= workingSize) {
            System.arraycopy(data, workingSize, dst, 0, workingSize);
        }
        return dst;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i < workingSize; i++) {
            action.accept(data[i]);
        }
    }
}
