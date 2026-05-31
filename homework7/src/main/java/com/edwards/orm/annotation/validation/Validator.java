package com.edwards.orm.annotation.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Validator {

    public static List<Violation> validateObject(Object object) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        List<Violation> violations = new ArrayList<>();

        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(object);

            for (Annotation annotation : field.getDeclaredAnnotations()) {
                Constraint constraint = annotation.annotationType().getAnnotation(Constraint.class);

                if (constraint != null) {
                    Constructor<?> validator = constraint.validateBy().getDeclaredConstructor();
                    validator.setAccessible(true);

                    @SuppressWarnings("unchecked")
                    ConstraintValidator<Object, Annotation> validatorInstance = (ConstraintValidator<Object, Annotation>) validator.newInstance();

                    if (!validatorInstance.isValid(value, annotation)) {
                        violations.add(
                                Violation.of(
                                        field,
                                        value,
                                        annotation
                                )
                        );
                    }
                }
            }
        }

        return violations;
    }
}
