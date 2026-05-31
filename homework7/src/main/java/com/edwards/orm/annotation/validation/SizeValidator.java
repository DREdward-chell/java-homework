package com.edwards.orm.annotation.validation;

import java.lang.annotation.Annotation;

public class SizeValidator implements ConstraintValidator<String, Size> {
    @Override
    public boolean isValid(String s, Size sizeAnnotation) {
        return s == null || (sizeAnnotation.min() <= s.length() && s.length() <= sizeAnnotation.max());
    }
}
