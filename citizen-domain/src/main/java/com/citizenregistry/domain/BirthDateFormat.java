package com.citizenregistry.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** The public date format is dd-MM-yyyy; uuuu makes strict parsing work without an era. */
public final class BirthDateFormat {
    /** Business rule: reject future birth dates and ages greater than 120 years. */
    public static final int MAX_AGE_YEARS = 120;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private BirthDateFormat() { }

    public static LocalDate parse(String value) {
        if (value == null || !value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) {
            throw new DateTimeParseException("Απαιτείται ημερομηνία dd-MM-yyyy", String.valueOf(value), 0);
        }
        LocalDate parsed = LocalDate.parse(value, FORMATTER);
        if (parsed.getYear() == 0) {
            throw new DateTimeParseException("Το έτος πρέπει να είναι μεγαλύτερο από μηδέν", value, 6);
        }
        if (!isPlausibleBirthDate(parsed)) {
            throw new DateTimeParseException(
                    "Η ημερομηνία γέννησης δεν μπορεί να είναι μελλοντική ή παλαιότερη από 120 έτη",
                    value, 0);
        }
        return parsed;
    }

    /** Used by both the CLI/parser and server-side Bean Validation. */
    public static boolean isPlausibleBirthDate(LocalDate date) {
        if (date == null) return false;
        LocalDate today = LocalDate.now();
        return !date.isAfter(today) && !date.isBefore(today.minusYears(MAX_AGE_YEARS));
    }
}
