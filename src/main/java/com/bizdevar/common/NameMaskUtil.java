package com.bizdevar.common;

public final class NameMaskUtil {

    private NameMaskUtil() {}

    /** "Ilqar Qasimov" -> "I** Q**" */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return "***";
        String[] parts = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append(' ');
            String part = parts[i];
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0))).append("**");
        }
        return out.length() == 0 ? "***" : out.toString();
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 1) return "*@" + domain;
        return local.charAt(0) + "***@" + domain;
    }
}
