package com.citizenregistry.client;

import com.citizenregistry.domain.BirthDateFormat;
import com.citizenregistry.domain.Citizen;
import com.citizenregistry.domain.ErrorResponse;
import com.citizenregistry.domain.UpdateCitizenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class CitizenClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(CitizenClientApplication.class, args);
    }
}

@Component
class CitizenConsole implements CommandLineRunner {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Scanner scanner = new Scanner(System.in);

    CitizenConsole(RestClient.Builder builder, ObjectMapper objectMapper,
                   @Value("${citizen.service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        clearScreen();
        while (true) {
            printMenu();
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();

            // Μη έγκυρη επιλογή: τερματισμός της εφαρμογής.
            if (!List.of("1", "2", "3", "4", "5", "6").contains(choice)) {
                System.out.println("Μη έγκυρη επιλογή. Τερματισμός.");
                return;
            }

            if ("6".equals(choice)) {
                clearScreen();
                System.out.println("Έξοδος από την εφαρμογή Μητρώου Πολιτών.");
                return;
            }

            // Ξεχωριστή οθόνη για κάθε λειτουργία: το παλιό μενού εξαφανίζεται.
            clearScreen();
            printOperationTitle(choice);
            try {
                switch (choice) {
                    case "1" -> create();
                    case "2" -> delete();
                    case "3" -> update();
                    case "4" -> search();
                    case "5" -> show();
                    default -> throw new IllegalStateException("Μη αναμενόμενη επιλογή");
                }
            } catch (Exception ex) {
                System.out.println("\nΣφάλμα: " + readableError(ex));
            }

            // Διατηρούμε το αποτέλεσμα (ή το μήνυμα σφάλματος) μέχρι να το διαβάσει ο χρήστης.
            System.out.print("\nΠατήστε Enter για επιστροφή στο μενού...");
            System.out.flush();
            if (!scanner.hasNextLine()) return;
            scanner.nextLine();
            clearScreen();
        }
    }

    private void clearScreen() {
        // ANSI clear + clear scrollback σε τυπικά Linux terminals / Windows Terminal.
        // Δεν στέλνουμε escape sequences όταν η είσοδος έχει ανακατευθυνθεί ή σε dumb terminals.
        // Για απενεργοποίηση: java -Dcitizen.console.clear=false -jar ...
        if (Boolean.parseBoolean(System.getProperty("citizen.console.clear", "true"))
                && System.console() != null
                && !"dumb".equalsIgnoreCase(System.getenv("TERM"))) {
            System.out.print("\u001B[H\u001B[2J\u001B[3J");
            System.out.flush();
        }
    }

    private void printMenu() {
        System.out.println("========================================");
        System.out.println("         ΜΗΤΡΩΟ ΠΟΛΙΤΩΝ");
        System.out.println("========================================");
        System.out.println("1. Εισαγωγή πολίτη");
        System.out.println("2. Διαγραφή πολίτη");
        System.out.println("3. Ενημέρωση πολίτη");
        System.out.println("4. Αναζήτηση");
        System.out.println("5. Εμφάνιση πολίτη");
        System.out.println("6. Έξοδος");
        System.out.print("\nΕπιλογή: ");
        System.out.flush();
    }

    private void printOperationTitle(String choice) {
        String operation = switch (choice) {
            case "1" -> "ΕΙΣΑΓΩΓΗ ΠΟΛΙΤΗ";
            case "2" -> "ΔΙΑΓΡΑΦΗ ΠΟΛΙΤΗ";
            case "3" -> "ΕΝΗΜΕΡΩΣΗ ΠΟΛΙΤΗ";
            case "4" -> "ΑΝΑΖΗΤΗΣΗ ΠΟΛΙΤΩΝ";
            case "5" -> "ΕΜΦΑΝΙΣΗ ΠΟΛΙΤΗ";
            default -> "";
        };
        System.out.println("========================================");
        System.out.println("  " + operation);
        System.out.println("========================================\n");
    }

    private void create() {
        Citizen c = new Citizen();
        c.setIdCard(readIdCard("ΑΤ (8 χαρακτήρες): "));
        c.setFirstName(readRequired("Όνομα: ", "Το όνομα είναι υποχρεωτικό."));
        c.setLastName(readRequired("Επίθετο: ", "Το επίθετο είναι υποχρεωτικό."));
        c.setGender(readRequired("Φύλο: ", "Το φύλο είναι υποχρεωτικό."));
        c.setBirthDate(readDate("Ημερομηνία γέννησης (dd-MM-yyyy): "));
        c.setAfm(readAfm("ΑΦΜ (9 ψηφία, Enter για κενό): "));
        c.setAddress(emptyToNull(readAddress("Διεύθυνση (Enter για κενό): ")));

        Citizen created = restClient.post().contentType(MediaType.APPLICATION_JSON).body(c)
                .retrieve().body(Citizen.class);
        System.out.println("\nΕπιτυχής εισαγωγή:");
        printCitizen(created);
    }

    private void delete() {
        String id = readIdCard("ΑΤ προς διαγραφή: ");
        restClient.delete().uri("/{id}", id).retrieve().toBodilessEntity();
        System.out.println("\nΗ εγγραφή διαγράφηκε.");
    }

    private void update() {
        String id = readIdCard("ΑΤ προς ενημέρωση: ");
        // Το PUT αντικαθιστά και τα δύο επεξεργάσιμα πεδία. Διαβάζουμε πρώτα τις
        // υπάρχουσες τιμές, ώστε το Enter να τις διατηρεί χωρίς να χαθούν δεδομένα.
        Citizen current = restClient.get().uri("/{id}", id).retrieve().body(Citizen.class);
        if (current == null) {
            throw new IllegalStateException("Η υπηρεσία επέστρεψε κενή εγγραφή.");
        }
        System.out.println("Τρέχουσες τιμές:");
        printCitizen(current);
        System.out.println("Enter = χωρίς αλλαγή, - = διαγραφή προαιρετικού πεδίου.");
        String afm = readUpdateAfm("Νέο ΑΦΜ (9 ψηφία / Enter / -): ");
        String address = readAddress("Νέα διεύθυνση (Enter / -): ");
        if (afm == null && address.isEmpty()) {
            System.out.println("Δεν ζητήθηκε καμία αλλαγή.");
            return;
        }
        UpdateCitizenRequest request = new UpdateCitizenRequest(
                afm == null ? current.getAfm() : (afm.equals("-") ? null : afm),
                address.isEmpty() ? current.getAddress() : (address.equals("-") ? null : address));
        Citizen updated = restClient.put().uri("/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(Citizen.class);
        System.out.println("\nΕπιτυχής ενημέρωση:");
        printCitizen(updated);
    }

    private void show() {
        String id = readIdCard("ΑΤ: ");
        Citizen citizen = restClient.get().uri("/{id}", id).retrieve().body(Citizen.class);
        System.out.println();
        printCitizen(citizen);
    }

    private void search() {
        System.out.println("Δώσε τιμές στα πεδία αναζήτησης. Enter = αγνόηση πεδίου.\n");
        String idCard = readOptionalIdCard("ΑΤ (Enter για αγνόηση): ");
        String firstName = emptyToNull(read("Όνομα: "));
        String lastName = emptyToNull(read("Επίθετο: "));
        String gender = emptyToNull(read("Φύλο: "));
        String birthDate = readOptionalDate("Ημερομηνία γέννησης (dd-MM-yyyy, Enter για αγνόηση): ");
        String afm = readAfm("ΑΦΜ (Enter για αγνόηση): ");
        String address = emptyToNull(read("Διεύθυνση: "));

        if (idCard == null && firstName == null && lastName == null && gender == null
                && birthDate == null && afm == null && address == null) {
            System.out.println("Σφάλμα: Δώστε τουλάχιστον ένα πεδίο αναζήτησης.");
            return;
        }

        List<Citizen> citizens = restClient.get().uri(builder -> {
            builder.path("/search");
            if (idCard != null) builder.queryParam("idCard", idCard);
            if (firstName != null) builder.queryParam("firstName", firstName);
            if (lastName != null) builder.queryParam("lastName", lastName);
            if (gender != null) builder.queryParam("gender", gender);
            if (birthDate != null) builder.queryParam("birthDate", birthDate);
            if (afm != null) builder.queryParam("afm", afm);
            if (address != null) builder.queryParam("address", address);
            return builder.build();
        }).retrieve().body(new ParameterizedTypeReference<List<Citizen>>() {});

        if (citizens == null || citizens.isEmpty()) {
            System.out.println("\nΔεν βρέθηκαν εγγραφές.");
        } else {
            System.out.println("\nΒρέθηκαν " + citizens.size() + " εγγραφές:");
            for (int i = 0; i < citizens.size(); i++) {
                System.out.println("\n--- Εγγραφή " + (i + 1) + " ---");
                printCitizen(citizens.get(i));
            }
        }
    }

    private void printCitizen(Citizen citizen) {
        if (citizen == null) {
            System.out.println("Η υπηρεσία επέστρεψε κενή εγγραφή.");
            return;
        }
        System.out.println("----------------------------------------");
        System.out.println("ΑΤ:                   " + citizen.getIdCard());
        System.out.println("Όνομα:                " + citizen.getFirstName());
        System.out.println("Επίθετο:              " + citizen.getLastName());
        System.out.println("Φύλο:                 " + citizen.getGender());
        System.out.println("Ημερομηνία γέννησης:  " +
                (citizen.getBirthDate() == null ? "-" : BirthDateFormat.FORMATTER.format(citizen.getBirthDate())));
        System.out.println("ΑΦΜ:                  " + optional(citizen.getAfm()));
        System.out.println("Διεύθυνση:            " + optional(citizen.getAddress()));
        System.out.println("----------------------------------------");
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String read(String prompt) {
        return readRaw(prompt).trim();
    }

    private String readRaw(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return scanner.nextLine();
    }

    private String readIdCard(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            if (value.matches("\\S{8}")) return value;
            System.out.println("Σφάλμα: Ο ΑΤ πρέπει να έχει ακριβώς 8 χαρακτήρες. Δοκιμάστε ξανά.");
        }
    }

    private String readOptionalIdCard(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            if (value.isEmpty()) return null;
            if (value.matches("\\S{8}")) return value;
            System.out.println("Σφάλμα: Ο ΑΤ πρέπει να έχει ακριβώς 8 χαρακτήρες ή να είναι κενός.");
        }
    }

    private String readAfm(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            if (value.isEmpty()) return null;
            if (value.matches("[0-9]{9}")) return value;
            System.out.println("Σφάλμα: Το ΑΦΜ πρέπει να περιέχει ακριβώς 9 ψηφία (0-9). Δοκιμάστε ξανά.");
        }
    }

    private String readAddress(String prompt) {
        while (true) {
            String value = read(prompt);
            if (value.length() <= 500) return value;
            System.out.println("Σφάλμα: Η διεύθυνση δεν μπορεί να υπερβαίνει τους 500 χαρακτήρες.");
        }
    }

    private String readUpdateAfm(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            if (value.isEmpty()) return null; // Διατήρηση υπάρχουσας τιμής
            if (value.equals("-") || value.matches("[0-9]{9}")) return value;
            System.out.println("Σφάλμα: Δώστε 9 ψηφία, Enter για διατήρηση ή - για διαγραφή.");
        }
    }

    private String readRequired(String prompt, String error) {
        while (true) {
            String value = read(prompt);
            if (!value.isBlank() && value.length() <= 255) return value;
            System.out.println("Σφάλμα: " + (value.isBlank() ? error : "Το πεδίο δεν μπορεί να υπερβαίνει τους 255 χαρακτήρες."));
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            try {
                if (!value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}"))
                    throw new DateTimeParseException("Μη έγκυρη μορφή", value, 0);
                return BirthDateFormat.parse(value);
            } catch (DateTimeParseException ex) {
                System.out.println("Σφάλμα: Δώστε πραγματική ημερομηνία dd-MM-yyyy, όχι μελλοντική και με ηλικία έως 120 ετών.");
            }
        }
    }

    private String readOptionalDate(String prompt) {
        while (true) {
            String value = readRaw(prompt);
            if (value.isEmpty()) return null;
            try {
                if (!value.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}"))
                    throw new DateTimeParseException("Μη έγκυρη μορφή", value, 0);
                BirthDateFormat.parse(value);
                return value;
            } catch (DateTimeParseException ex) {
                System.out.println("Σφάλμα: Δώστε πραγματική ημερομηνία dd-MM-yyyy, όχι μελλοντική, με ηλικία έως 120 ετών ή Enter για αγνόηση.");
            }
        }
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private String readableError(Exception ex) {
        if (ex instanceof RestClientResponseException http) {
            try {
                ErrorResponse error = objectMapper.readValue(http.getResponseBodyAsString(), ErrorResponse.class);
                StringBuilder text = new StringBuilder(http.getStatusCode() + " - " + error.message());
                Map<String, String> details = error.validationErrors();
                if (details != null) details.forEach((field, message) ->
                        text.append("\n  ").append(field).append(": ").append(message));
                return text.toString();
            } catch (Exception ignored) {
                return http.getStatusCode() + " - " + http.getResponseBodyAsString();
            }
        }
        if (ex instanceof ResourceAccessException) {
            return "Αδυναμία σύνδεσης με τη REST υπηρεσία. Ελέγξτε ότι εκτελείται στο http://localhost:8080.";
        }
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }
}
