package com.citizenregistry.domain;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Enforces a plausible birth date even when the Citizen is built without JSON parsing. */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BirthDateValidator.class)
public @interface ValidBirthDate {
    String message() default "Η ημερομηνία γέννησης πρέπει να είναι έως 120 έτη πριν από σήμερα και όχι μελλοντική";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
