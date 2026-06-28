package com.bizdevar.profile;

import com.bizdevar.common.Json;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final JdbcTemplate jdbc;

    public ProfileController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> get(HttpServletRequest request) {
        AppUser u = AuthSupport.require(request);
        return build(u);
    }

    @PutMapping
    public Map<String, Object> update(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        AppUser u = AuthSupport.require(request);
        String firstName = str(body.get("first_name"));
        String lastName = str(body.get("last_name"));
        Object prefs = body.get("notif_prefs");
        String prefsJson = prefs == null ? "{}" : Json.write(prefs);

        jdbc.update("INSERT INTO user_profiles (user_id, first_name, last_name, notif_prefs_json) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE first_name = VALUES(first_name), last_name = VALUES(last_name), "
                        + "notif_prefs_json = VALUES(notif_prefs_json)",
                u.id, firstName, lastName, prefsJson);
        return build(u);
    }

    private Map<String, Object> build(AppUser u) {
        Map<String, Object> p = new LinkedHashMap<>();

        String firstName = "";
        String lastName = "";
        String[] parts = (u.name == null ? "" : u.name).trim().split("\\s+", 2);
        if (parts.length > 0) firstName = parts[0];
        if (parts.length > 1) lastName = parts[1];

        Map<String, Object> notif = new LinkedHashMap<>();
        notif.put("emailNotif", true);
        notif.put("smsNotif", false);
        notif.put("whatsappNotif", true);

        List<Map<String, Object>> rows = jdbc.query(
                "SELECT first_name, last_name, notif_prefs_json FROM user_profiles WHERE user_id = ?",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fn", rs.getString("first_name"));
                    m.put("ln", rs.getString("last_name"));
                    m.put("prefs", rs.getString("notif_prefs_json"));
                    return m;
                }, u.id);
        if (!rows.isEmpty()) {
            String fn = (String) rows.get(0).get("fn");
            String ln = (String) rows.get(0).get("ln");
            if (fn != null && !fn.isBlank()) firstName = fn;
            if (ln != null && !ln.isBlank()) lastName = ln;
            Map<String, Boolean> np = Json.readBoolMap((String) rows.get(0).get("prefs"));
            if (!np.isEmpty()) {
                notif.clear();
                notif.putAll(np);
            }
        }

        p.put("first_name", firstName);
        p.put("last_name", lastName);
        p.put("email", u.email);
        p.put("phone", u.phone == null ? "" : u.phone);
        p.put("addresses", addresses(u.id));
        p.put("notif_prefs", notif);
        return p;
    }

    private List<Map<String, Object>> addresses(long userId) {
        return jdbc.query("SELECT id, label, address, lat, lng FROM addresses WHERE user_id = ?",
                (rs, n) -> {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("id", rs.getLong("id"));
                    a.put("label", rs.getString("label"));
                    a.put("address", rs.getString("address"));
                    Object lat = rs.getObject("lat");
                    Object lng = rs.getObject("lng");
                    if (lat != null) a.put("lat", ((Number) lat).doubleValue());
                    if (lng != null) a.put("lng", ((Number) lng).doubleValue());
                    return a;
                }, userId);
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
