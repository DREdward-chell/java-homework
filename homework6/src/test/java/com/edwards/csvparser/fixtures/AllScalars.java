package com.edwards.csvparser.fixtures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllScalars {
    private int i;
    private Integer iBoxed;
    private long l;
    private Long lBoxed;
    private double d;
    private Double dBoxed;
    private boolean b;
    private Boolean bBoxed;
    private String s;
}
