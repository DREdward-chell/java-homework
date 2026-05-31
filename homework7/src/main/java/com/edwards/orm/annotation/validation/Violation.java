package com.edwards.orm.annotation.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public record Violation(
        String field,
        Object invalidValue,
        String message,
        Class<? extends Annotation> annotation
) {

    private static String getMessageFromAnnotation(Annotation annotation) {
        try {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            return (String) annotationType.getMethod("message").invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return "Validation failed";
        }
    }

    static Violation of(Field field, Object value, Annotation annotation) {
        return new Violation(
                field.getName(),
                value,
                getMessageFromAnnotation(annotation),
                annotation.annotationType()
        );
    }
}