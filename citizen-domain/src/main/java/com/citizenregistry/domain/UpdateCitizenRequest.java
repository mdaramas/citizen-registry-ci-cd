package com.citizenregistry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Δεδομένα PUT: και τα δύο επεξεργάσιμα πεδία πρέπει να υπάρχουν στο JSON.
 * Η τιμή null ή το κενό κείμενο αφαιρεί την αντίστοιχη προαιρετική τιμή.
 */
public class UpdateCitizenRequest {

    @Pattern(regexp = "([0-9]{9})?", message = "Το ΑΦΜ πρέπει να περιέχει ακριβώς 9 ψηφία ή να είναι κενό")
    private String afm;

    @Size(max = 500, message = "Η διεύθυνση δεν μπορεί να υπερβαίνει τους 500 χαρακτήρες")
    private String address;

    private boolean afmProvided;
    private boolean addressProvided;

    public UpdateCitizenRequest() { }

    public UpdateCitizenRequest(String afm, String address) {
        setAfm(afm);
        setAddress(address);
    }

    public String getAfm() { return afm; }
    public void setAfm(String afm) {
        this.afm = afm;
        this.afmProvided = true;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
        this.addressProvided = true;
    }

    @JsonIgnore
    public boolean isAfmProvided() { return afmProvided; }

    @JsonIgnore
    public boolean isAddressProvided() { return addressProvided; }
}
