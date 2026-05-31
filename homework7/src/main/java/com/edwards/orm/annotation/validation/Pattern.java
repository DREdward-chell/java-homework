package com.edwards.orm.annotation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validateBy = PatternValidator.class)
public @interface Pattern {
    String regex();
    String message() default "value does not match pattern";
}
