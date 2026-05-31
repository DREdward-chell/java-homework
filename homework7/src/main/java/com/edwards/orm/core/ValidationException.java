package com.edwards.orm.core;

import com.edwards.orm.annotation.validation.Violation;

import java.util.List;

public class ValidationException extends OrmException {
    private final List<Violation> violations;

    public ValidationException(List<Violation> violations) {
        super("Validation failed: " + violations);
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
