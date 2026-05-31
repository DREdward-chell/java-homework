package com.edwards.orm.annotation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validateBy = SizeValidator.class)
public @interface Size {
    int min();
    int max();
    String message() default "the string object doesn't fit in given constraints";
}
