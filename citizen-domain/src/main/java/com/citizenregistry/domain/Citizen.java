package com.citizenregistry.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "citizens")
public class Citizen {

    @Id
    @Column(name = "id_card", length = 8, nullable = false, updatable = false)
    @NotBlank(message = "Ο ΑΤ είναι υποχρεωτικός")
    @Size(min = 8, max = 8, message = "Ο ΑΤ πρέπει να αποτελείται ακριβώς από 8 χαρακτήρες")
    @Pattern(regexp = "\\S{8}", message = "Ο ΑΤ δεν επιτρέπεται να περιέχει κενά")
    private String idCard;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    @Size(max = 255, message = "Το όνομα δεν μπορεί να υπερβαίνει τους 255 χαρακτήρες")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Το επίθετο είναι υποχρεωτικό")
    @Size(max = 255, message = "Το επίθετο δεν μπορεί να υπερβαίνει τους 255 χαρακτήρες")
    private String lastName;

    @Column(nullable = false)
    @NotBlank(message = "Το φύλο είναι υποχρεωτικό")
    @Size(max = 255, message = "Το φύλο δεν μπορεί να υπερβαίνει τους 255 χαρακτήρες")
    private String gender;

    @Column(name = "birth_date", nullable = false)
    @NotNull(message = "Η ημερομηνία γέννησης είναι υποχρεωτική")
    @ValidBirthDate
    @JsonFormat(pattern = "dd-MM-uuuu", lenient = OptBoolean.FALSE)
    @JsonDeserialize(using = StrictLocalDateDeserializer.class)
    private LocalDate birthDate;

    @Column(length = 9)
    @Pattern(regexp = "[0-9]{9}", message = "Το ΑΦΜ πρέπει να αποτελείται από 9 ψηφία")
    private String afm;

    @Column(length = 500)
    @Size(max = 500, message = "Η διεύθυνση δεν μπορεί να υπερβαίνει τους 500 χαρακτήρες")
    private String address;

    public Citizen() {
    }

    public Citizen(String idCard, String firstName, String lastName, String gender,
                   LocalDate birthDate, String afm, String address) {
        this.idCard = idCard;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.afm = afm;
        this.address = address;
    }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getAfm() { return afm; }
    public void setAfm(String afm) { this.afm = afm; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Citizen citizen)) return false;
        return Objects.equals(idCard, citizen.idCard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCard);
    }

    @Override
    public String toString() {
        return "Citizen{" +
                "idCard='" + idCard + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", gender='" + gender + '\'' +
                ", birthDate=" + birthDate +
                ", afm='" + afm + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
