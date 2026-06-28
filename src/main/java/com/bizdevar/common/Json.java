package com.bizdevar.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON saxlanan TEXT sutunlari (images_json, specs_json, delivery_json ve s.) ucun yardimci.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {}

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static List<String> readStringList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public static Map<String, String> readStringMap(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(raw, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static Map<String, Object> readObject(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static Map<String, Boolean> readBoolMap(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(raw, new TypeReference<LinkedHashMap<String, Boolean>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
