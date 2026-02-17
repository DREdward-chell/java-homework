package com.edwards;

import com.edwards.collections.capacitors.IncrementalStrategy;
import com.edwards.collections.capacitors.RealExponentialStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CapacitorsTest {
    @Test
    public void testIncrementalStrategy() {
        var strategy = new IncrementalStrategy(1);

        var imaginaryCapacity = 10;

        assertEquals(imaginaryCapacity + 1, strategy.calculateNewCapacity(imaginaryCapacity, imaginaryCapacity + 1));
    }

    @Test
    public void testIncrementalStrategy2() {
        var strategy = new IncrementalStrategy(4);
        var imaginaryCapacity = 10;
        assertEquals(imaginaryCapacity + 8, strategy.calculateNewCapacity(imaginaryCapacity, imaginaryCapacity + 5));
    }

    @Test
    public void testExponentialStrategy() {
        var strategy2 = new RealExponentialStrategy(2);

        assertEquals(16, strategy2.calculateNewCapacity(1, 11));
    }
}
