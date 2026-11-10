package com.citizenregistry.service.service;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;
import com.citizenregistry.service.exception.CitizenAlreadyExistsException;
import com.citizenregistry.service.exception.CitizenNotFoundException;
import com.citizenregistry.service.exception.InvalidRequestException;
import com.citizenregistry.service.repository.CitizenRepository;
import com.citizenregistry.service.specification.CitizenSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CitizenRegistryServiceImpl implements CitizenRegistryService {
    private final CitizenRepository repository;

    public CitizenRegistryServiceImpl(CitizenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Citizen create(Citizen citizen) {
        if (repository.existsById(citizen.getIdCard())) {
            throw new CitizenAlreadyExistsException("Υπάρχει ήδη πολίτης με ΑΤ " + citizen.getIdCard());
        }
        return repository.saveAndFlush(citizen);
    }

    @Override
    public void delete(String idCard) {
        repository.delete(getByIdCard(idCard));
    }

    @Override
    public Citizen update(String idCard, UpdateCitizenRequest request) {
        if (!request.isAfmProvided() || !request.isAddressProvided()) {
            throw new InvalidRequestException("Το PUT απαιτεί και τα δύο πεδία: afm και address (επιτρέπεται null)");
        }
        Citizen citizen = getByIdCard(idCard);
        citizen.setAfm(emptyToNull(request.getAfm()));
        citizen.setAddress(emptyToNull(request.getAddress()));
        return repository.saveAndFlush(citizen);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    @Transactional(readOnly = true)
    public Citizen getByIdCard(String idCard) {
        return repository.findById(idCard)
                .orElseThrow(() -> new CitizenNotFoundException("Δεν βρέθηκε πολίτης με ΑΤ " + idCard));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Citizen> search(String idCard, String firstName, String lastName, String gender,
                                LocalDate birthDate, String afm, String address) {
        Specification<Citizen> spec = Specification.where(null);
        if (idCard != null) spec = spec.and(CitizenSpecifications.idCardEquals(idCard));
        if (firstName != null) spec = spec.and(CitizenSpecifications.firstNameEqualsIgnoreCase(firstName));
        if (lastName != null) spec = spec.and(CitizenSpecifications.lastNameEqualsIgnoreCase(lastName));
        if (gender != null) spec = spec.and(CitizenSpecifications.genderEqualsIgnoreCase(gender));
        if (birthDate != null) spec = spec.and(CitizenSpecifications.birthDateEquals(birthDate));
        if (afm != null) spec = spec.and(CitizenSpecifications.afmEquals(afm));
        if (address != null) spec = spec.and(CitizenSpecifications.addressContainsIgnoreCase(address));
        return repository.findAll(spec);
    }
}
