package com.edwards.csvparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvCollection {
    String DEFAULT_DELIMITER = "|";

    String delimiter() default DEFAULT_DELIMITER;
}
