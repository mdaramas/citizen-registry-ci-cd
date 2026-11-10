package com.citizenregistry.service.unit;

import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CitizenJsonTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Test
    void serializesAndDeserializesStrictDate() throws Exception {
        Citizen citizen = new Citizen("ΑΒ123456", "Γιώργος", "Παπαδόπουλος", "Άνδρας",
                LocalDate.of(1970, 10, 10), null, null);
        String json = mapper.writeValueAsString(citizen);
        assertThat(json).contains("\"birthDate\":\"10-10-1970\"");
        assertThat(mapper.readValue(json, Citizen.class).getBirthDate()).isEqualTo(LocalDate.of(1970, 10, 10));
        assertThatThrownBy(() -> mapper.readValue(json.replace("10-10-1970", "31-02-1970"), Citizen.class))
                .hasMessageContaining("Μη έγκυρη ημερομηνία");
        assertThatThrownBy(() -> mapper.readValue(json.replace("10-10-1970", "1970-10-10"), Citizen.class))
                .hasMessageContaining("Μη έγκυρη ημερομηνία");
        assertThatThrownBy(() -> mapper.readValue(json.replace("10-10-1970", "09-09-1000"), Citizen.class))
                .hasMessageContaining("Μη έγκυρη ημερομηνία");
    }

    @Test
    void putJsonIncludesNullAndTracksRequiredProperties() throws Exception {
        UpdateCitizenRequest missing = mapper.readValue("{\"address\":\"Πάτρα\"}", UpdateCitizenRequest.class);
        assertThat(missing.isAddressProvided()).isTrue();
        assertThat(missing.isAfmProvided()).isFalse();
        UpdateCitizenRequest clear = mapper.readValue("{\"afm\":null,\"address\":null}", UpdateCitizenRequest.class);
        assertThat(clear.isAfmProvided()).isTrue();
        assertThat(clear.isAddressProvided()).isTrue();
        assertThat(clear.getAfm()).isNull();
        assertThat(clear.getAddress()).isNull();
        String json = mapper.writeValueAsString(new UpdateCitizenRequest(null, "Πάτρα"));
        assertThat(json).contains("\"afm\":null", "\"address\":\"Πάτρα\"");
        assertThat(json).doesNotContain("Provided");
        assertThatThrownBy(() -> mapper.readValue("{\"firstName\":\"X\"}", UpdateCitizenRequest.class))
                .hasMessageContaining("firstName");
    }

}
