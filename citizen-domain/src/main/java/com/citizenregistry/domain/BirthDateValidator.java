package com.citizenregistry.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate> {
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        // @NotNull handles missing dates; this validator checks the allowed date range.
        return value == null || BirthDateFormat.isPlausibleBirthDate(value);
    }
}
