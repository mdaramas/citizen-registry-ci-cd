package com.citizenregistry.service.unit;

import com.citizenregistry.domain.BirthDateFormat;
import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CitizenValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() { factory.close(); }

    private Citizen valid() {
        return new Citizen("AB123456", "Μαρία", "Ιωάννου", "Γυναίκα",
                LocalDate.of(1988, 3, 20), "123456789", "Αθήνα");
    }

    @Test
    void constructorsGettersSettersAndEquality() {
        Citizen c = valid();
        assertThat(c.getIdCard()).isEqualTo("AB123456");
        assertThat(c.getFirstName()).isEqualTo("Μαρία");
        assertThat(c.getLastName()).isEqualTo("Ιωάννου");
        assertThat(c.getGender()).isEqualTo("Γυναίκα");
        assertThat(c.getBirthDate()).isEqualTo(LocalDate.of(1988, 3, 20));
        assertThat(c.getAfm()).isEqualTo("123456789");
        assertThat(c.getAddress()).isEqualTo("Αθήνα");
        Citizen blank = new Citizen();
        blank.setIdCard("AB123456");
        blank.setFirstName("Νίκη");
        blank.setLastName("Παπαδοπούλου");
        blank.setGender("Γυναίκα");
        blank.setBirthDate(LocalDate.of(1999, 12, 1));
        blank.setAfm(null);
        blank.setAddress("Πάτρα");
        assertThat(blank.getFirstName()).isEqualTo("Νίκη");
        assertThat(blank.getAddress()).isEqualTo("Πάτρα");
        assertThat(blank).isEqualTo(c).hasSameHashCodeAs(c);
        assertThat(c.toString()).contains("AB123456");
    }

    @Test
    void validAndOptionalFields() {
        assertThat(validator.validate(valid())).isEmpty();
        Citizen c = valid();
        c.setAfm(null);
        c.setAddress(null);
        assertThat(validator.validate(c)).isEmpty();
    }

    @Test
    void idCardExactlyEightAndRequired() {
        for (String id : new String[]{null, "", " ", "ABC", "ABCDEFGHI", "AB 12345"}) {
            Citizen c = valid(); c.setIdCard(id); assertFieldViolation(c, "idCard");
        }
    }

    @Test
    void mandatoryFieldsAreNotBlank() {
        Citizen c = valid(); c.setFirstName(" "); assertFieldViolation(c, "firstName");
        c = valid(); c.setLastName(""); assertFieldViolation(c, "lastName");
        c = valid(); c.setGender(null); assertFieldViolation(c, "gender");
        c = valid(); c.setBirthDate(null); assertFieldViolation(c, "birthDate");
    }

    @Test
    void afmExactlyNineDigitsWhenProvided() {
        for (String afm : new String[]{"", "12345", "1234567890", "12345678A"}) {
            Citizen c = valid(); c.setAfm(afm); assertFieldViolation(c, "afm");
        }
    }

    @Test
    void addressLengthIsValidated() {
        Citizen c = valid();
        c.setAddress("x".repeat(501));
        assertFieldViolation(c, "address");
    }

    @Test
    void putDtoValidatesAfmAndTracksBothProperties() {
        UpdateCitizenRequest req = new UpdateCitizenRequest();
        assertThat(req.isAfmProvided()).isFalse();
        assertThat(req.isAddressProvided()).isFalse();
        req.setAddress("Πάτρα");
        assertThat(req.isAddressProvided()).isTrue();
        assertThat(req.isAfmProvided()).isFalse();
        req.setAfm("12345");
        assertThat(validator.validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("afm"));
        req.setAfm(""); // Η κενή τιμή αφαιρεί το προαιρετικό ΑΦΜ.
        assertThat(validator.validate(req)).isEmpty();
        UpdateCitizenRequest clear = new UpdateCitizenRequest(null, null);
        assertThat(clear.isAfmProvided()).isTrue();
        assertThat(clear.isAddressProvided()).isTrue();
        assertThat(validator.validate(clear)).isEmpty();
    }

    @Test
    void dateParsingIsStrictAndAcceptsLeapDay() {
        assertThat(BirthDateFormat.parse("10-10-1970")).isEqualTo(LocalDate.of(1970, 10, 10));
        assertThat(BirthDateFormat.parse("29-02-2024")).isEqualTo(LocalDate.of(2024, 2, 29));
        for (String date : new String[]{"31-02-1990", "29-02-2023", "1-01-1990", "01-1-1990", "1990-01-01", "00-01-1990"}) {
            assertThatThrownBy(() -> BirthDateFormat.parse(date)).isInstanceOf(DateTimeParseException.class);
        }
    }

    @Test
    void birthDateRejectsImplausibleYearsAndFutureDates() {
        LocalDate today = LocalDate.now();
        assertThat(BirthDateFormat.isPlausibleBirthDate(today)).isTrue();
        assertThat(BirthDateFormat.isPlausibleBirthDate(today.minusYears(120))).isTrue();
        assertThat(BirthDateFormat.isPlausibleBirthDate(today.minusYears(120).minusDays(1))).isFalse();
        assertThat(BirthDateFormat.isPlausibleBirthDate(today.plusDays(1))).isFalse();
        assertThatThrownBy(() -> BirthDateFormat.parse("09-09-1000"))
                .isInstanceOf(DateTimeParseException.class);
        String future = BirthDateFormat.FORMATTER.format(today.plusDays(1));
        assertThatThrownBy(() -> BirthDateFormat.parse(future))
                .isInstanceOf(DateTimeParseException.class);
        Citizen c = valid();
        c.setBirthDate(LocalDate.of(1000, 9, 9));
        assertFieldViolation(c, "birthDate");
        c.setBirthDate(today.plusDays(1));
        assertFieldViolation(c, "birthDate");
    }

    private void assertFieldViolation(Citizen citizen, String field) {
        Set<ConstraintViolation<Citizen>> violations = validator.validate(citizen);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }
}
