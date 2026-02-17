package com.edwards.collections.capacitors;

public interface CapacityStrategy {
    /**
     * Вычисляет новую емкость массива на основе текущей.
     *
     * @param currentCapacity текущая емкость массива
     * @param requiredCapacity минимально необходимая емкость
     * @return новая емкость массива
     */
    int calculateNewCapacity(int currentCapacity, int requiredCapacity);
}
