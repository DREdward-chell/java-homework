package com.edwards.orm.annotation.validation;

public class MinValidator implements ConstraintValidator<Number, Min> {
    @Override
    public boolean isValid(Number value, Min annotation) {
        return value == null || value.doubleValue() >= annotation.value();
    }
}
