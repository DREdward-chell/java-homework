package com.edwards.orm.annotation.validation;

public class MaxValidator implements ConstraintValidator<Number, Max> {
    @Override
    public boolean isValid(Number value, Max annotation) {
        return value == null || value.doubleValue() <= annotation.value();
    }
}
