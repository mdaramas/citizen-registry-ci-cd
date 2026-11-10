package com.citizenregistry.service.config;

import com.citizenregistry.service.CitizenServiceApplication;
import com.citizenregistry.service.controller.CitizenController;
import com.citizenregistry.service.repository.CitizenRepository;
import com.citizenregistry.service.service.CitizenRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CitizenServiceApplication.class)
class ApplicationContextTest {

    @Autowired CitizenController controller;
    @Autowired CitizenRegistryService service;
    @Autowired CitizenRepository repository;

    @Test
    void contextLoadsAndCoreBeansExist() {
        assertThat(controller).isNotNull();
        assertThat(service).isNotNull();
        assertThat(repository).isNotNull();
    }
}
