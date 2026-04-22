package com.edwards.gpucalc.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistogramTest {

    @Test
    void singlePlotIncrementsCountAndColor() {
        Histogram h = new Histogram(4, 3);
        h.plot(2, 1, 0.5, 0.25, 1.0);
        assertThat(h.count(2, 1)).isEqualTo(1L);
        assertThat(h.rSum(2, 1)).isEqualTo(0.5);
        assertThat(h.gSum(2, 1)).isEqualTo(0.25);
        assertThat(h.bSum(2, 1)).isEqualTo(1.0);
    }

    @Test
    void multiplePlotsAtSameCellAccumulate() {
        Histogram h = new Histogram(4, 3);
        h.plot(1, 1, 0.1, 0.2, 0.3);
        h.plot(1, 1, 0.2, 0.2, 0.2);
        assertThat(h.count(1, 1)).isEqualTo(2L);
        assertThat(h.rSum(1, 1)).isCloseTo(0.3, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(h.gSum(1, 1)).isCloseTo(0.4, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(h.bSum(1, 1)).isCloseTo(0.5, org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void emptyHistogramMaxAndTotalAreZero() {
        Histogram h = new Histogram(2, 2);
        assertThat(h.maxCount()).isZero();
        assertThat(h.totalCount()).isZero();
    }

    @Test
    void maxCountTracksHighestBin() {
        Histogram h = new Histogram(3, 3);
        h.plot(0, 0, 0, 0, 0);
        h.plot(0, 0, 0, 0, 0);
        h.plot(2, 2, 0, 0, 0);
        assertThat(h.maxCount()).isEqualTo(2L);
        assertThat(h.totalCount()).isEqualTo(3L);
    }

    @Test
    void inBoundsChecksAllFourEdges() {
        Histogram h = new Histogram(4, 3);
        assertThat(h.inBounds(0, 0)).isTrue();
        assertThat(h.inBounds(3, 2)).isTrue();
        assertThat(h.inBounds(-1, 0)).isFalse();
        assertThat(h.inBounds(0, -1)).isFalse();
        assertThat(h.inBounds(4, 0)).isFalse();
        assertThat(h.inBounds(0, 3)).isFalse();
    }

    @Test
    void mergeFromSumsBins() {
        Histogram a = new Histogram(2, 2);
        Histogram b = new Histogram(2, 2);
        a.plot(0, 0, 1, 0, 0);
        b.plot(0, 0, 0, 1, 0);
        b.plot(1, 1, 0, 0, 1);
        a.mergeFrom(b);
        assertThat(a.count(0, 0)).isEqualTo(2L);
        assertThat(a.rSum(0, 0)).isEqualTo(1.0);
        assertThat(a.gSum(0, 0)).isEqualTo(1.0);
        assertThat(a.count(1, 1)).isEqualTo(1L);
        assertThat(a.bSum(1, 1)).isEqualTo(1.0);
    }

    @Test
    void mergeRejectsShapeMismatch() {
        Histogram a = new Histogram(2, 2);
        Histogram b = new Histogram(3, 2);
        assertThatThrownBy(() -> a.mergeFrom(b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNonPositiveSize() {
        assertThatThrownBy(() -> new Histogram(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Histogram(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawArraysLengthIsWidthTimesHeight() {
        Histogram h = new Histogram(5, 7);
        assertThat(h.counts()).hasSize(35);
        assertThat(h.redAccumulator()).hasSize(35);
        assertThat(h.greenAccumulator()).hasSize(35);
        assertThat(h.blueAccumulator()).hasSize(35);
    }
}
