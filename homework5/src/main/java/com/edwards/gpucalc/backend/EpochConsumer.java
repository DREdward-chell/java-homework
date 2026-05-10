package com.edwards.gpucalc.backend;

import com.edwards.gpucalc.core.Histogram;

@FunctionalInterface
public interface EpochConsumer {
    void accept(Histogram snapshot, int index, int totalEpochs);
}
