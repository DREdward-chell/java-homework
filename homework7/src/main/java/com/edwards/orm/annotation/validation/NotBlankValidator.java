package com.edwards.orm.annotation.validation;

public class NotBlankValidator implements ConstraintValidator<String, NotBlank> {
    @Override
    public boolean isValid(String value, NotBlank annotation) {
        return value == null || !value.isBlank();
    }
}
