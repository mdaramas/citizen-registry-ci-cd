package com.citizenregistry.service.service;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;

import java.time.LocalDate;
import java.util.List;

public interface CitizenRegistryService {
    Citizen create(Citizen citizen);
    void delete(String idCard);
    Citizen update(String idCard, UpdateCitizenRequest request);
    Citizen getByIdCard(String idCard);
    List<Citizen> search(String idCard, String firstName, String lastName, String gender,
                         LocalDate birthDate, String afm, String address);
}
