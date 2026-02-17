package com.edwards.collections;

import java.util.*;
import java.util.function.Consumer;

/**
 * Большинство методов DynamicArray это прямая копирка
 * того, что существует в Java STD, поэтому я решил, что
 * лучшим решением будет сделать {@link DynamicArray} коллекцией.
 * Так я могу избежать лишнего кода и написать только те
 * методы, что напрямую связаны с упорядоченными коллекциями.
 * Добавление {@link Collection} также способствует лучшей совместимости
 * со стандартными функциями.
 * <p>Помимо этого, я также решил что будет лучше если я реализую
 * {@link DynamicArray} на {@code GenericType}, так как по сути
 * код менять не потребуется, а сама коллекция станет логичнее
 */
public interface DynamicArray<T> extends Collection<T> {
    T[] getData();

    T get(int index) throws IndexOutOfBoundsException;

    T set(int index, T element) throws IndexOutOfBoundsException;

    boolean add(int index, T element);

    boolean addAll(int index, Collection<? extends T> c) throws IndexOutOfBoundsException;

    T remove(int index) throws IndexOutOfBoundsException;

    int indexOf(Object o);

    int lastIndexOf(Object o);

    @SafeVarargs
    static <T> DynamicArray<T> of(T... elements) {
        return new CVector<>(elements);
    }

    default <C extends Iterable<? extends T>> boolean collectionEquals(C other) {
        Iterator<T> it1 = iterator();
        Iterator<? extends T> it2 = other.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            if (!it1.next().equals(it2.next())) {
                return false;
            }
        }
        return it1.hasNext() == it2.hasNext();
    }

    default void sort() {
        Arrays.sort(getData(), 0, size());
    }

    default Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public T next() {
                return get(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                for (int i = index + 1; i < size(); i++) {
                    action.accept(get(i));
                }
            }

            T getValue() {
                return get(index);
            }

            T setValue(int index, T element) {
                return set(index, element);
            }
        };
    }

    default Spliterator<T> spliterator() {
        return Arrays.stream(getData(), 0, size()).spliterator();
    }
}