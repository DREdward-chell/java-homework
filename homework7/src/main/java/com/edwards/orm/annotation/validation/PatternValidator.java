package com.edwards.orm.annotation.validation;

public class PatternValidator implements ConstraintValidator<String, Pattern> {
    @Override
    public boolean isValid(String value, Pattern annotation) {
        return value == null || value.matches(annotation.regex());
    }
}
