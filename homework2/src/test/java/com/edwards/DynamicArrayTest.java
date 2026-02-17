package com.edwards;

import com.edwards.collections.CVector;
import com.edwards.collections.DynamicArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DynamicArrayTest {
    @Test
    public void testAdd() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.add(6);

        assertTrue(DynamicArray.of(1, 2, 3, 4, 5, 6).collectionEquals(vector));
    }

    @Test
    public void testAddIndex() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.add(1, 5);

        assertTrue(DynamicArray.of(1, 5, 2, 3, 4, 5).collectionEquals(vector));
    }

    @Test
    public void testAddAll() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.addAll(DynamicArray.of(1, 2, 3, 4, 5));
        assertTrue(DynamicArray.of(1, 2, 3, 4, 5, 1, 2, 3, 4, 5).collectionEquals(vector));
    }

    @Test
    public void testAddAllIndex() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.addAll(1, DynamicArray.of(1, 2, 3, 4, 5));
        assertTrue(DynamicArray.of(1, 1, 2, 3, 4, 5, 2, 3, 4, 5).collectionEquals(vector));
        assertEquals(10, vector.size());
        vector.addAll(vector.size() - 1, DynamicArray.of(6, 7));
        assertTrue(DynamicArray.of(1, 1, 2, 3, 4, 5, 2, 3, 4, 6, 7, 5).collectionEquals(vector));
    }

    @Test
    public void testRemove() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.remove(0);
        vector.remove(1);

        assertTrue(DynamicArray.of(2, 4, 5).collectionEquals(vector));
    }

    @Test
    public void testClear() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.clear();
        vector.add(6);
        assertTrue(DynamicArray.of(6).collectionEquals(vector));
    }

    @Test
    public void testSize() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        vector.clear();
        vector.add(6);
        assertEquals(1, vector.size());
    }

    @Test
    public void testIterator() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        StringBuilder out = new StringBuilder();
        for (Integer i : vector) {
            out.append(i);
        }
        assertEquals("12345", out.toString());
    }

    @Test
    public void testContains() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        assertTrue(vector.contains(1));
        assertTrue(vector.contains(3));
        assertFalse(vector.contains(6));
    }

    @Test
    public void testIndexOf() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        assertEquals(1, vector.indexOf(2));
        assertEquals(2, vector.indexOf(3));
    }

    @Test
    public void testArrayCreation() {
        var vector = new CVector<>();
        vector.add(1);
        vector.add(2);
        vector.add(3);
        assertEquals(3, vector.size());
        assertArrayEquals(new Object[]{1, 2, 3}, vector.toArray());
    }

    @Test
    public void testSort() {
        var vector = DynamicArray.of(1, 5, 3, 4, 2);
        vector.sort();
        assertTrue(DynamicArray.of(1, 2, 3, 4, 5).collectionEquals(vector));
    }

    @Test
    public void testStreamApi() {
        var vector = DynamicArray.of(1, 2, 3, 4, 5);
        var res = new CVector<Integer>();
        vector.stream().map(x -> x * x).filter(x -> x % 2 == 0).forEach(res::add);
        assertTrue(DynamicArray.of(4, 16).collectionEquals(res));
    }
}
