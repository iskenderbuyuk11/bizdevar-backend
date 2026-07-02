package com.bizdevar.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtil {

    private SlugUtil() {}

    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = s.replace("ə", "e").replace("ı", "i").replace("ö", "o").replace("ü", "u")
                .replace("ş", "s").replace("ç", "c").replace("ğ", "g").replace("İ", "i").replace("Ə", "e");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", "");
        return s;
    }

    public static String personSlug(String firstName, String lastName) {
        return slugify(trim(firstName) + trim(lastName));
    }

    public static String personSlug(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return slugify(parts[0]);
        return slugify(parts[0] + parts[parts.length - 1]);
    }

    public static String uniqueSlug(String base, Predicate<String> exists) {
        String slug = slugify(base);
        if (slug.isEmpty()) slug = "magaza";
        if (!exists.test(slug)) return slug;
        for (int i = 2; i < 1000; i++) {
            String candidate = slug + i;
            if (!exists.test(candidate)) return candidate;
        }
        return slug + System.currentTimeMillis() % 10000;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
