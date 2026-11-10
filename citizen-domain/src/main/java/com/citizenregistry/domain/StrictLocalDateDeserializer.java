package com.citizenregistry.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Rejects impossible dates and any representation other than dd-MM-yyyy. */
public class StrictLocalDateDeserializer extends StdDeserializer<LocalDate> {
    public StrictLocalDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            throw JsonMappingException.from(parser, "Η ημερομηνία πρέπει να είναι κείμενο dd-MM-yyyy");
        }
        try {
            return BirthDateFormat.parse(parser.getText());
        } catch (DateTimeParseException ex) {
            throw JsonMappingException.from(parser, "Μη έγκυρη ημερομηνία γέννησης (dd-MM-yyyy): πραγματική ημερομηνία, όχι μελλοντική και εντός 120 ετών", ex);
        }
    }
}
