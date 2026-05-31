package com.edwards.orm.annotation.validation;

import java.lang.annotation.Annotation;

public interface ConstraintValidator<T, A extends Annotation> {
    boolean isValid(T value, A annotation);
}
