package com.edwards.collections.capacitors;

public class ExponentialStrategy implements CapacityStrategy {
    int exponent;

    public ExponentialStrategy(int exponent) throws IllegalArgumentException {
        if (exponent <= 1) {
            throw new IllegalArgumentException("Exponent must be greater than 1");
        }
        this.exponent = exponent;
    }

    @Override
    public int calculateNewCapacity(int currentCapacity, int requiredCapacity) {
        double ratio = (double) requiredCapacity / currentCapacity;
        int multiplier = (int) Math.ceil(Math.log(ratio) / Math.log(exponent));

        long newCapacity = (long) currentCapacity * (long) Math.pow(exponent, multiplier);

        if (newCapacity > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) newCapacity;
    }
}
