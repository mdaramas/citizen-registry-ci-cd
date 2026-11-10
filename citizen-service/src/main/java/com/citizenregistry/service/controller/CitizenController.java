package com.citizenregistry.service.controller;

import com.citizenregistry.domain.BirthDateFormat;
import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.UpdateCitizenRequest;
import com.citizenregistry.service.exception.InvalidRequestException;
import com.citizenregistry.service.service.CitizenRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {
    private static final Set<String> SEARCH_FIELDS = Set.of(
            "idCard", "firstName", "lastName", "gender", "birthDate", "afm", "address");

    private final CitizenRegistryService service;

    public CitizenController(CitizenRegistryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Citizen> create(@Valid @RequestBody Citizen citizen) {
        Citizen saved = service.create(citizen);
        return ResponseEntity.created(URI.create("/api/citizens/" + saved.getIdCard())).body(saved);
    }

    @DeleteMapping("/{idCard}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String idCard) {
        service.delete(requireValidId(idCard));
    }

    @PutMapping("/{idCard}")
    public Citizen update(@PathVariable String idCard, @Valid @RequestBody UpdateCitizenRequest request) {
        return service.update(requireValidId(idCard), request);
    }

    @GetMapping("/{idCard}")
    public Citizen getByIdCard(@PathVariable String idCard) {
        return service.getByIdCard(requireValidId(idCard));
    }

    @DeleteMapping(path = {"", "/"})
    public void deleteWithoutIdCard() {
        throw new InvalidRequestException("Ο ΑΤ είναι υποχρεωτικός για διαγραφή");
    }

    @PutMapping(path = {"", "/"})
    public Citizen updateWithoutIdCard() {
        throw new InvalidRequestException("Ο ΑΤ είναι υποχρεωτικός για ενημέρωση");
    }

    @GetMapping(path = {"", "/"})
    public Citizen getWithoutIdCard() {
        throw new InvalidRequestException("Ο ΑΤ είναι υποχρεωτικός για εμφάνιση πολίτη");
    }

    @GetMapping("/search")
    public List<Citizen> search(@RequestParam MultiValueMap<String, String> parameters) {
        if (parameters.isEmpty()) {
            throw new InvalidRequestException("Πρέπει να δοθεί τουλάχιστον ένα πεδίο αναζήτησης");
        }
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (!SEARCH_FIELDS.contains(entry.getKey())) {
                throw new InvalidRequestException("Άγνωστο πεδίο αναζήτησης: " + entry.getKey());
            }
            if (entry.getValue().size() != 1) {
                throw new InvalidRequestException("Κάθε πεδίο αναζήτησης επιτρέπεται μόνο μία φορά: " + entry.getKey());
            }
            if (entry.getValue().get(0) == null || entry.getValue().get(0).isBlank()) {
                throw new InvalidRequestException("Το πεδίο αναζήτησης " + entry.getKey() + " δεν επιτρέπεται να είναι κενό");
            }
            int max = entry.getKey().equals("address") ? 500 : 255;
            if (entry.getValue().get(0).length() > max) {
                throw new InvalidRequestException("Υπερβολικά μεγάλο πεδίο αναζήτησης: " + entry.getKey());
            }
        }

        String idCard = parameters.getFirst("idCard");
        if (idCard != null) requireValidId(idCard);
        String afm = parameters.getFirst("afm");
        if (afm != null && !afm.matches("[0-9]{9}")) {
            throw new InvalidRequestException("Το ΑΦΜ πρέπει να αποτελείται από ακριβώς 9 ψηφία");
        }
        LocalDate birthDate = null;
        String rawDate = parameters.getFirst("birthDate");
        if (rawDate != null) {
            try {
                birthDate = BirthDateFormat.parse(rawDate);
            } catch (DateTimeParseException ex) {
                throw new InvalidRequestException("Μη έγκυρη ημερομηνία αναζήτησης: απαιτείται dd-MM-yyyy");
            }
        }
        return service.search(idCard, value(parameters, "firstName"), value(parameters, "lastName"),
                value(parameters, "gender"), birthDate, afm, value(parameters, "address"));
    }

    private String value(MultiValueMap<String, String> params, String name) {
        String value = params.getFirst(name);
        return value == null ? null : value.trim();
    }

    private String requireValidId(String value) {
        if (value == null || !value.matches("\\S{8}")) {
            throw new InvalidRequestException("Ο ΑΤ πρέπει να αποτελείται από ακριβώς 8 χαρακτήρες");
        }
        return value;
    }
}
