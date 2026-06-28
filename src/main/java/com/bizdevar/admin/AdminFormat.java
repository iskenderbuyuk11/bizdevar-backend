package com.bizdevar.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Admin paneli ucun status etiketleri ve metrik kartlar. */
public final class AdminFormat {

    private AdminFormat() {}

    public static Map<String, Object> metric(String label, String value, String delta, String trend) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("delta", delta);
        m.put("trend", trend);
        return m;
    }

    public static String azn(double v) {
        return "₼ " + String.format("%,.0f", v);
    }

    public static String azn2(double v) {
        return "₼ " + String.format("%,.2f", v);
    }

    public static String statusLabel(String s) {
        if (s == null) return "";
        return switch (s) {
            case "placed" -> "Verildi";
            case "packing" -> "Hazirlanir";
            case "cargo" -> "Kargodadir";
            case "delivering" -> "Catdirilir";
            case "delivered" -> "Tamamlandi";
            case "cancelled" -> "Legv edildi";
            case "active" -> "Aktiv";
            case "pending" -> "Gozleyir";
            case "review" -> "Yoxlamada";
            case "restricted" -> "Mehdud";
            case "published" -> "Yayimlanib";
            case "draft" -> "Qaralama";
            default -> s;
        };
    }

    public static String statusType(String s) {
        if (s == null) return "info";
        return switch (s) {
            case "delivered", "active", "published" -> "success";
            case "placed", "packing", "cargo", "delivering", "review" -> "warning";
            case "cancelled", "restricted", "complaint" -> "danger";
            default -> "info";
        };
    }

    public static String verLabel(String v) {
        if (v == null) return "";
        return switch (v) {
            case "verified" -> "Verified";
            case "document_pending" -> "Sened gozleyir";
            case "risk_review" -> "Risk yoxlamasi";
            case "pending" -> "Gozleyir";
            default -> v;
        };
    }

    public static String verType(String v) {
        if ("verified".equals(v)) return "success";
        if ("risk_review".equals(v)) return "danger";
        return "warning";
    }

    public static String productStatusLabel(String s) {
        if (s == null) return "";
        return switch (s) {
            case "pending" -> "Tesdiq gozleyir";
            case "active" -> "Aktiv";
            case "moderation" -> "Moderasiya";
            case "complaint" -> "Sikayet var";
            default -> s;
        };
    }

    public static String productStatusType(String s) {
        if (s == null) return "info";
        return switch (s) {
            case "active" -> "success";
            case "pending", "moderation" -> "warning";
            case "complaint" -> "danger";
            default -> "info";
        };
    }
}
