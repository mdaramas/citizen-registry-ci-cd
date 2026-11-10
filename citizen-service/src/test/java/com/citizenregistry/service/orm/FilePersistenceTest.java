package com.citizenregistry.service.orm;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.service.CitizenServiceApplication;
import com.citizenregistry.service.repository.CitizenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FilePersistenceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void citizenSurvivesClosingAndReopeningTheApplicationContext() {
        String dbFile = temporaryDirectory.resolve("citizensdb").toAbsolutePath().toString().replace('\\', '/');
        String[] properties = {
                "--spring.datasource.url=jdbc:h2:file:" + dbFile,
                "--spring.jpa.hibernate.ddl-auto=update",
                "--spring.h2.console.enabled=false",
                "--spring.main.banner-mode=off"
        };
        try (ConfigurableApplicationContext first = new SpringApplicationBuilder(CitizenServiceApplication.class)
                .web(WebApplicationType.NONE).run(properties)) {
            first.getBean(CitizenRepository.class).saveAndFlush(new Citizen("ΑΒ123456", "Γιώργος", "Παπαδόπουλος",
                    "Άνδρας", LocalDate.of(1970, 10, 10), "123456789", "Αθήνα"));
        }
        assertThat(Files.exists(Path.of(dbFile + ".mv.db"))).isTrue();
        try (ConfigurableApplicationContext second = new SpringApplicationBuilder(CitizenServiceApplication.class)
                .web(WebApplicationType.NONE).run(properties)) {
            Citizen restored = second.getBean(CitizenRepository.class).findById("ΑΒ123456").orElseThrow();
            assertThat(restored.getFirstName()).isEqualTo("Γιώργος");
            assertThat(restored.getBirthDate()).isEqualTo(LocalDate.of(1970, 10, 10));
        }
    }
}
