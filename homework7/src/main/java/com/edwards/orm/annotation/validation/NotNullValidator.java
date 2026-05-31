package com.edwards.orm.annotation.validation;

public class NotNullValidator implements ConstraintValidator<Object, NotNull> {
    @Override
    public boolean isValid(Object value, NotNull annotation) {
        return value != null;
    }
}
