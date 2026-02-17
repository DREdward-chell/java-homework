package com.edwards.collections.capacitors;

public class IncrementalStrategy implements CapacityStrategy {
    int fixedIncrement;

    public IncrementalStrategy(int fixedIncrement) throws IllegalArgumentException {
        if (fixedIncrement < 1) {
            throw new IllegalArgumentException("Fixed Increment must be greater than 1");
        }
        this.fixedIncrement = fixedIncrement;
    }

    @Override
    public int calculateNewCapacity(int currentCapacity, int requiredCapacity) {
        if (requiredCapacity <= currentCapacity) {
            return currentCapacity;
        }
        return currentCapacity + fixedIncrement;
    }
}
