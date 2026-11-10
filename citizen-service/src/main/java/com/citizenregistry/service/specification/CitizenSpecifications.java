package com.citizenregistry.service.specification;

import com.citizenregistry.domain.Citizen;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

public final class CitizenSpecifications {
    private CitizenSpecifications() {}

    public static Specification<Citizen> idCardEquals(String value) {
        return (root, query, cb) -> cb.equal(root.get("idCard"), value);
    }

    public static Specification<Citizen> firstNameEqualsIgnoreCase(String value) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("firstName")), value.toLowerCase(Locale.ROOT));
    }

    public static Specification<Citizen> lastNameEqualsIgnoreCase(String value) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("lastName")), value.toLowerCase(Locale.ROOT));
    }

    public static Specification<Citizen> genderEqualsIgnoreCase(String value) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("gender")), value.toLowerCase(Locale.ROOT));
    }

    public static Specification<Citizen> birthDateEquals(LocalDate value) {
        return (root, query, cb) -> cb.equal(root.get("birthDate"), value);
    }

    public static Specification<Citizen> afmEquals(String value) {
        return (root, query, cb) -> cb.equal(root.get("afm"), value);
    }

    public static Specification<Citizen> addressContainsIgnoreCase(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("address")),
                "%" + value.toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%", '\\');
    }
}
