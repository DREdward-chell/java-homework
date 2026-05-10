package com.edwards.csvparser.fixtures;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Cyclic {
    @SuppressWarnings("unused")
    private Cyclic next;
}
