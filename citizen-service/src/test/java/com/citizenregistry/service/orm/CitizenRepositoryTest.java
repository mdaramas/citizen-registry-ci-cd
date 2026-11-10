package com.citizenregistry.service.orm;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.service.repository.CitizenRepository;
import com.citizenregistry.service.specification.CitizenSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CitizenRepositoryTest {
    @Autowired CitizenRepository repository;
    @Autowired TestEntityManager entityManager;

    private Citizen citizen(String id, String first, String address) {
        return new Citizen(id, first, "Παπαδόπουλος", "Άνδρας",
                LocalDate.of(1990, 5, 12), "123456789", address);
    }

    @Test
    void persistsReadsUpdatesAndDeletesThroughOrm() {
        repository.saveAndFlush(citizen("ΑΒ123456", "Γιώργος", "Αθήνα"));
        entityManager.clear();
        Citizen loaded = repository.findById("ΑΒ123456").orElseThrow();
        assertThat(loaded.getFirstName()).isEqualTo("Γιώργος");
        assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 12));
        loaded.setAddress("Πάτρα");
        repository.saveAndFlush(loaded);
        entityManager.clear();
        assertThat(repository.findById("ΑΒ123456").orElseThrow().getAddress()).isEqualTo("Πάτρα");
        repository.deleteById("ΑΒ123456");
        repository.flush();
        entityManager.clear();
        assertThat(repository.findById("ΑΒ123456")).isEmpty();
    }

    @Test
    void specificationsSupportMultipleMatchesAndCombinations() {
        repository.saveAndFlush(citizen("AB123456", "Γιώργος", "Αθήνα"));
        repository.saveAndFlush(citizen("CD654321", "Νίκος", "Πάτρα"));
        entityManager.clear();
        assertThat(repository.findAll(CitizenSpecifications.lastNameEqualsIgnoreCase("ΠΑΠΑΔΌΠΟΥΛΟΣ")))
                .hasSize(2);
        assertThat(repository.findAll(CitizenSpecifications.lastNameEqualsIgnoreCase("Παπαδόπουλος")
                .and(CitizenSpecifications.firstNameEqualsIgnoreCase("Νίκος"))))
                .extracting(Citizen::getIdCard).containsExactly("CD654321");
        assertThat(repository.findAll(CitizenSpecifications.addressContainsIgnoreCase("ΑΘ"))).hasSize(1);
    }
}
