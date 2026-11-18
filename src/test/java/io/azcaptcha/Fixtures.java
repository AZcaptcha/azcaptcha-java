package io.azcaptcha;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

final class Fixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private Fixtures() {
    }

    static Map<String, Object> load(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + name + ".json")) {
            if (in == null) {
                throw new IllegalStateException("missing fixture " + name);
            }
            return MAPPER.readValue(in, MAP_TYPE);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> body(String name) {
        Map<String, Object> response = (Map<String, Object>) load(name).get("response");
        return (Map<String, Object>) response.get("json");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> task(String name) {
        Map<String, Object> request = (Map<String, Object>) load(name).get("request");
        Map<String, Object> json = (Map<String, Object>) request.get("json");
        return (Map<String, Object>) json.get("task");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> form(String name) {
        Map<String, Object> request = (Map<String, Object>) load(name).get("request");
        return (Map<String, Object>) request.get("form");
    }

    static List<?> codes(String section) {
        Map<String, Object> spec = load("errors");
        return List.copyOf(((Map<String, Object>) spec.get(section)).keySet());
    }
}
