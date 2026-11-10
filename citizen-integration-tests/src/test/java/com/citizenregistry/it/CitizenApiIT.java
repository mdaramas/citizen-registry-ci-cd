package com.citizenregistry.it;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.service.CitizenServiceApplication;
import com.citizenregistry.service.repository.CitizenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CitizenServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:integrationdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=false",
        "spring.jackson.deserialization.fail-on-unknown-properties=true"
})
class CitizenApiIT {
    @LocalServerPort int port;
    @Autowired CitizenRepository repository;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api/citizens";
        repository.deleteAll();
    }

    private Citizen citizen(String id, String first, String last, String afm) {
        return new Citizen(id, first, last, "Άνδρας", LocalDate.of(1990, 1, 15), afm, "Αθήνα");
    }

    private String json(Citizen c) {
        try { return mapper.writeValueAsString(c); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @Test
    void postCreatesCitizenAndRejectsDuplicateWithoutOverwriting() {
        Citizen c = citizen("AB123456", "Γιώργος", "Παπαδόπουλος", "123456789");
        given().contentType(ContentType.JSON).body(json(c)).when().post().then()
                .statusCode(201).header("Location", containsString("/api/citizens/AB123456"))
                .body("idCard", equalTo("AB123456"));
        given().contentType(ContentType.JSON).body(json(c)).when().post().then()
                .statusCode(409).body("message", containsString("Υπάρχει ήδη"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void postRejectsMissingInvalidAndUnknownFields() {
        Citizen invalidId = citizen("ABC", "Γιώργος", "Παπαδόπουλος", "123456789");
        given().contentType(ContentType.JSON).body(json(invalidId)).when().post().then()
                .statusCode(400).body("validationErrors.idCard", notNullValue());
        Citizen invalidAfm = citizen("AB123456", "Γιώργος", "Παπαδόπουλος", "12345");
        given().contentType(ContentType.JSON).body(json(invalidAfm)).when().post().then()
                .statusCode(400).body("validationErrors.afm", notNullValue());
        String missingFirstName = """
                {"idCard":"AB123456","firstName":" ","lastName":"Παπαδόπουλος",
                 "gender":"Άνδρας","birthDate":"15-01-1990"}
                """;
        given().contentType(ContentType.JSON).body(missingFirstName).when().post().then()
                .statusCode(400).body("validationErrors.firstName", notNullValue());
        String missingBirthDate = """
                {"idCard":"AB123456","firstName":"Γιώργος","lastName":"Παπαδόπουλος","gender":"Άνδρας"}
                """;
        given().contentType(ContentType.JSON).body(missingBirthDate).when().post().then()
                .statusCode(400).body("validationErrors.birthDate", notNullValue());
        String invalidDate = """
                {"idCard":"AB123456","firstName":"Γιώργος","lastName":"Παπαδόπουλος",
                 "gender":"Άνδρας","birthDate":"31-02-1990"}
                """;
        given().contentType(ContentType.JSON).body(invalidDate).when().post().then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body(missingFirstName.replace("\"firstName\":\" \"", "\"firstName\":\"Γιώργος\",\"unexpected\":true"))
                .when().post().then().statusCode(400);
        assertThat(repository.count()).isZero();
    }

    @Test
    void postRejectsAncientAndFutureBirthDates() {
        String valid = json(citizen("AB123456", "George", "Papadopoulos", null));
        given().contentType(ContentType.JSON)
                .body(valid.replace("15-01-1990", "09-09-1000"))
                .when().post().then().statusCode(400);
        String future = com.citizenregistry.domain.BirthDateFormat.FORMATTER
                .format(LocalDate.now().plusDays(1));
        given().contentType(ContentType.JSON)
                .body(valid.replace("15-01-1990", future))
                .when().post().then().statusCode(400);
        assertThat(repository.count()).isZero();
    }

    @Test
    void postAndGetAcceptExactDateFormatAndOptionalFields() {
        String request = """
                {"idCard":"AB123456","firstName":"George","lastName":"Papadopoulos",
                 "gender":"Male","birthDate":"10-10-1970"}
                """;
        given().contentType(ContentType.JSON).body(request).when().post().then()
                .statusCode(201).body("birthDate", equalTo("10-10-1970"));
        given().when().get("/{id}", "AB123456").then().statusCode(200)
                .body("birthDate", equalTo("10-10-1970"))
                .body("afm", nullValue()).body("address", nullValue());
        given().when().get("/{id}", "ZZ999999").then().statusCode(404);
        given().when().get("/{id}", "ABC").then().statusCode(400);
        given().when().get().then().statusCode(400);
    }

    @Test
    void putReplacesEditableFieldsRejectsIncompletePayloadAndDoesNotSupportPatch() {
        repository.save(citizen("AB123456", "Γιώργος", "Παπαδόπουλος", "123456789"));
        String updated = "{\"afm\":\"987654321\",\"address\":\"Πάτρα\"}";
        given().contentType(ContentType.JSON).body(updated)
                .when().put("/{id}", "AB123456").then().statusCode(200)
                .body("afm", equalTo("987654321")).body("address", equalTo("Πάτρα"))
                .body("firstName", equalTo("Γιώργος"));
        // Το PUT είναι επαναλήψιμο χωρίς αλλαγή της τελικής κατάστασης.
        given().contentType(ContentType.JSON).body(updated)
                .when().put("/{id}", "AB123456").then().statusCode(200)
                .body("afm", equalTo("987654321")).body("address", equalTo("Πάτρα"));
        given().contentType(ContentType.JSON).body("{\"afm\":null,\"address\":\"Πάτρα\"}")
                .when().put("/{id}", "AB123456").then().statusCode(200)
                .body("afm", nullValue()).body("address", equalTo("Πάτρα"));
        given().contentType(ContentType.JSON).body("{\"afm\":\"12345\",\"address\":null}")
                .when().put("/{id}", "AB123456").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"afm\":null,\"address\":null,\"firstName\":\"Changed\"}")
                .when().put("/{id}", "AB123456").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{}")
                .when().put("/{id}", "AB123456").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"afm\":null}")
                .when().put("/{id}", "AB123456").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"address\":null}")
                .when().put("/{id}", "AB123456").then().statusCode(400);
        given().contentType(ContentType.JSON).body(updated)
                .when().put("/{id}", "ZZ999999").then().statusCode(404);
        given().contentType(ContentType.JSON).body(updated)
                .when().put("/{id}", "ABC").then().statusCode(400);
        given().contentType(ContentType.JSON).body(updated)
                .when().put().then().statusCode(400);
        given().contentType(ContentType.JSON).body(updated)
                .when().patch("/{id}", "AB123456").then().statusCode(405);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById("AB123456").orElseThrow().getFirstName()).isEqualTo("Γιώργος");
    }

    @Test
    void searchSupportsEveryFieldAndCombinedCriteriaWithMultipleMatches() {
        repository.save(citizen("AB123456", "Γιώργος", "Παπαδόπουλος", "123456789"));
        repository.save(citizen("CD654321", "Νίκος", "Παπαδόπουλος", "987654321"));
        given().queryParam("lastName", "Παπαδόπουλος").when().get("/search").then()
                .statusCode(200).body("size()", equalTo(2));
        given().queryParam("lastName", "Παπαδόπουλος").queryParam("firstName", "Νίκος")
                .when().get("/search").then().statusCode(200).body("size()", equalTo(1))
                .body("[0].idCard", equalTo("CD654321"));
        given().queryParam("idCard", "AB123456").when().get("/search").then().statusCode(200)
                .body("size()", equalTo(1));
        given().queryParam("gender", "Άνδρας").when().get("/search").then().statusCode(200)
                .body("size()", equalTo(2));
        given().queryParam("birthDate", "15-01-1990").when().get("/search").then().statusCode(200)
                .body("size()", equalTo(2));
        given().queryParam("afm", "123456789").when().get("/search").then().statusCode(200)
                .body("[0].idCard", equalTo("AB123456"));
        given().queryParam("address", "θήν").when().get("/search").then().statusCode(200)
                .body("size()", equalTo(2));
        given().queryParam("lastName", "Άγνωστος").when().get("/search").then().statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void searchRejectsInvalidMissingRepeatedAndUnknownCriteria() {
        given().when().get("/search").then().statusCode(400);
        given().queryParam("idCard", "ABC").when().get("/search").then().statusCode(400);
        given().queryParam("afm", "12A456789").when().get("/search").then().statusCode(400);
        given().queryParam("birthDate", "31-02-1990").when().get("/search").then().statusCode(400);
        given().queryParam("birthDate", "1-01-1990").when().get("/search").then().statusCode(400);
        given().queryParam("firstName", " ").when().get("/search").then().statusCode(400);
        given().queryParam("unknown", "anything").when().get("/search").then().statusCode(400);
        given().when().get("/search?firstName=George&firstName=Maria").then().statusCode(400);
    }

    @Test
    void deleteRequiresValidExistingId() {
        repository.save(citizen("AB123456", "Γιώργος", "Παπαδόπουλος", "123456789"));
        given().when().delete("/{id}", "AB123456").then().statusCode(204);
        given().when().delete("/{id}", "AB123456").then().statusCode(404);
        given().when().delete("/{id}", "ABC").then().statusCode(400);
        given().when().delete().then().statusCode(400);
        assertThat(repository.count()).isZero();
    }
}
