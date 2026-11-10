package com.citizenregistry.service.unit;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;
import com.citizenregistry.service.exception.CitizenAlreadyExistsException;
import com.citizenregistry.service.exception.CitizenNotFoundException;
import com.citizenregistry.service.exception.InvalidRequestException;
import com.citizenregistry.service.repository.CitizenRepository;
import com.citizenregistry.service.service.CitizenRegistryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenRegistryServiceTest {
    @Mock CitizenRepository repository;
    @InjectMocks CitizenRegistryServiceImpl service;
    private Citizen citizen;

    @BeforeEach
    void setup() {
        citizen = new Citizen("ΑΒ123456", "Γιώργος", "Παπαδόπουλος", "Άνδρας",
                LocalDate.of(1970, 10, 10), "123456789", "Αθήνα");
    }

    @Test
    void duplicateDoesNotSave() {
        when(repository.existsById("ΑΒ123456")).thenReturn(true);
        assertThatThrownBy(() -> service.create(citizen)).isInstanceOf(CitizenAlreadyExistsException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void nonexistentIdCannotBeReadOrDeleted() {
        when(repository.findById("ΑΒ123456")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByIdCard("ΑΒ123456")).isInstanceOf(CitizenNotFoundException.class);
        assertThatThrownBy(() -> service.delete("ΑΒ123456")).isInstanceOf(CitizenNotFoundException.class);
        verify(repository, never()).delete(any(Citizen.class));
    }

    @Test
    void putReplacesBothEditableFieldsAndPreservesIdentity() {
        when(repository.findById("ΑΒ123456")).thenReturn(Optional.of(citizen));
        when(repository.saveAndFlush(citizen)).thenReturn(citizen);
        UpdateCitizenRequest request = new UpdateCitizenRequest("987654321", "Πάτρα");
        Citizen updated = service.update("ΑΒ123456", request);
        assertThat(updated.getAfm()).isEqualTo("987654321");
        assertThat(updated.getAddress()).isEqualTo("Πάτρα");
        assertThat(updated.getFirstName()).isEqualTo("Γιώργος");
        assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(1970, 10, 10));
    }

    @Test
    void putRejectsMissingEitherFieldWithoutRepositoryCall() {
        assertThatThrownBy(() -> service.update("ΑΒ123456", new UpdateCitizenRequest()))
                .isInstanceOf(InvalidRequestException.class);
        UpdateCitizenRequest onlyAfm = new UpdateCitizenRequest();
        onlyAfm.setAfm("987654321");
        assertThatThrownBy(() -> service.update("ΑΒ123456", onlyAfm))
                .isInstanceOf(InvalidRequestException.class);
        UpdateCitizenRequest onlyAddress = new UpdateCitizenRequest();
        onlyAddress.setAddress("Πάτρα");
        assertThatThrownBy(() -> service.update("ΑΒ123456", onlyAddress))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void putExplicitNullClearsOptionalFields() {
        when(repository.findById("ΑΒ123456")).thenReturn(Optional.of(citizen));
        when(repository.saveAndFlush(citizen)).thenReturn(citizen);
        Citizen updated = service.update("ΑΒ123456", new UpdateCitizenRequest(null, null));
        assertThat(updated.getAfm()).isNull();
        assertThat(updated.getAddress()).isNull();
    }

    @Test
    void putRepeatedWithSameValuesIsIdempotent() {
        when(repository.findById("ΑΒ123456")).thenReturn(Optional.of(citizen));
        when(repository.saveAndFlush(citizen)).thenReturn(citizen);
        UpdateCitizenRequest request = new UpdateCitizenRequest("987654321", "Πάτρα");
        service.update("ΑΒ123456", request);
        service.update("ΑΒ123456", request);
        assertThat(citizen.getAfm()).isEqualTo("987654321");
        assertThat(citizen.getAddress()).isEqualTo("Πάτρα");
        verify(repository, times(2)).saveAndFlush(citizen);
    }

}
