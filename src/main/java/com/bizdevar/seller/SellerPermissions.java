package com.bizdevar.seller;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SellerPermissions {

    public static final String DASHBOARD = "dashboard";
    public static final String PRODUCTS = "products";
    public static final String FEEDBACK = "feedback";
    public static final String NOTIFICATIONS = "notifications";
    public static final String STAFF = "staff";
    public static final String SETTINGS = "settings";
    public static final String QUESTIONS = "questions";

    public static final List<String> ALL = List.of(
            DASHBOARD, PRODUCTS, FEEDBACK, NOTIFICATIONS, STAFF, SETTINGS, QUESTIONS);

    public static final List<String> MANAGER_DEFAULT = List.of(
            DASHBOARD, PRODUCTS, FEEDBACK, NOTIFICATIONS, QUESTIONS, SETTINGS);

    public static final List<String> STAFF_DEFAULT = List.of(
            DASHBOARD, PRODUCTS, FEEDBACK, NOTIFICATIONS, QUESTIONS);

    private SellerPermissions() {}

    public static Set<String> all() {
        return new LinkedHashSet<>(ALL);
    }

    public static Set<String> forRole(String role, List<String> custom) {
        if ("manager".equalsIgnoreCase(role)) {
            return new LinkedHashSet<>(MANAGER_DEFAULT);
        }
        if (custom != null && !custom.isEmpty()) {
            Set<String> out = new LinkedHashSet<>();
            for (String p : custom) {
                if (ALL.contains(p)) out.add(p);
            }
            if (!out.contains(DASHBOARD)) out.add(DASHBOARD);
            return out;
        }
        return new LinkedHashSet<>(STAFF_DEFAULT);
    }

    public static boolean can(Set<String> perms, String key) {
        return perms != null && perms.contains(key);
    }

    public static Map<String, Object> catalog() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dashboard", "Dashboard");
        m.put("products", "Məhsullar");
        m.put("feedback", "Rəy və şikayət");
        m.put("notifications", "Bildirişlər");
        m.put("staff", "İdarə heyəti");
        m.put("settings", "Tənzimləmələr");
        m.put("questions", "Suallar");
        return m;
    }
}
