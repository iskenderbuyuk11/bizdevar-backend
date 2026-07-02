package com.bizdevar.seller;

import com.bizdevar.common.ApiException;
import com.bizdevar.common.Json;
import com.bizdevar.common.NameMaskUtil;
import com.bizdevar.common.SlugUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class SellerStaffRepository {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public SellerStaffRepository(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.encoder = passwordEncoder;
    }

    public List<Map<String, Object>> listMaskedMembersForLogin(long sellerId, String ownerName, String ownerSlug,
                                                               SellerAccount owner) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> ownerRow = new LinkedHashMap<>();
        ownerRow.put("id", "owner");
        ownerRow.put("masked_name", NameMaskUtil.maskName(ownerName));
        ownerRow.put("username_slug", ownerSlug == null ? "" : ownerSlug);
        ownerRow.put("role", "owner");
        ownerRow.put("label", "Magaza sahibi");
        ownerRow.put("needs_password_setup",
                owner == null || owner.ownerLoginPasswordHash == null || owner.ownerLoginPasswordHash.isBlank());
        ownerRow.put("face_enrolled", owner != null && owner.ownerFaceEnrolled);
        out.add(ownerRow);

        List<Map<String, Object>> staff = jdbc.query(
                "SELECT id, name, role, username_slug, password_hash, face_enrolled FROM seller_staff "
                        + "WHERE seller_id = ? AND status = 'active' ORDER BY name, id",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(rs.getLong("id")));
                    m.put("masked_name", NameMaskUtil.maskName(rs.getString("name")));
                    m.put("username_slug", rs.getString("username_slug"));
                    m.put("role", rs.getString("role"));
                    m.put("label", roleLabel(rs.getString("role")));
                    String ph = rs.getString("password_hash");
                    m.put("needs_password_setup", ph == null || ph.isBlank());
                    m.put("face_enrolled", rs.getInt("face_enrolled") == 1);
                    return m;
                }, sellerId);
        out.addAll(staff);
        return out;
    }

    public List<Map<String, Object>> listMembersForLogin(long sellerId, String ownerName) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("id", "owner");
        owner.put("name", ownerName == null || ownerName.isBlank() ? "Mağaza sahibi" : ownerName);
        owner.put("role", "owner");
        owner.put("label", "Mağaza sahibi");
        out.add(owner);

        List<Map<String, Object>> staff = jdbc.query(
                "SELECT id, name, role FROM seller_staff WHERE seller_id = ? AND status = 'active' ORDER BY name, id",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(rs.getLong("id")));
                    m.put("name", rs.getString("name"));
                    m.put("role", rs.getString("role"));
                    m.put("label", roleLabel(rs.getString("role")));
                    return m;
                }, sellerId);
        out.addAll(staff);
        return out;
    }

    public List<Map<String, Object>> listStaff(long sellerId) {
        return jdbc.query(
                "SELECT id, email, name, role, status, permissions_json, invited_at, joined_at "
                        + "FROM seller_staff WHERE seller_id = ? ORDER BY invited_at DESC",
                (rs, n) -> mapRow(rs), sellerId);
    }

    public Optional<SellerStaff> findById(long staffId) {
        List<SellerStaff> list = jdbc.query(
                "SELECT * FROM seller_staff WHERE id = ?",
                (rs, n) -> mapEntity(rs), staffId);
        return list.stream().findFirst();
    }

    public Optional<SellerStaff> findByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        List<SellerStaff> list = jdbc.query(
                "SELECT * FROM seller_staff WHERE invite_token = ? AND status = 'invited'",
                (rs, n) -> mapEntity(rs), token.trim());
        return list.stream().findFirst();
    }

    public long invite(long sellerId, String email, String name, String role, Set<String> permissions) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank() || !normalized.contains("@")) {
            throw ApiException.badRequest("Duzgun email daxil edin");
        }
        String safeRole = normalizeRole(role);
        if ("owner".equals(safeRole)) {
            throw ApiException.badRequest("Sahib rolu teyin oluna bilmez");
        }
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM seller_staff WHERE seller_id = ? AND email = ?",
                Integer.class, sellerId, normalized);
        if (exists != null && exists > 0) {
            throw ApiException.conflict("Bu email artiq devet edilib");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        String usernameSlug = uniqueStaffSlug(name, sellerId);
        jdbc.update(
                "INSERT INTO seller_staff (seller_id, email, name, role, status, permissions_json, invite_token, invite_expires_at, username_slug) "
                        + "VALUES (?, ?, ?, ?, 'invited', ?, ?, DATE_ADD(NOW(), INTERVAL 7 DAY), ?)",
                sellerId, normalized, name == null ? "" : name.trim(), safeRole,
                Json.write(new ArrayList<>(SellerPermissions.forRole(safeRole,
                        permissions == null ? null : new ArrayList<>(permissions)))),
                token, usernameSlug);
        return jdbc.queryForObject(
                "SELECT id FROM seller_staff WHERE seller_id = ? AND email = ? ORDER BY id DESC LIMIT 1",
                Long.class, sellerId, normalized);
    }

    public String inviteTokenForStaff(long staffId) {
        return jdbc.queryForObject("SELECT invite_token FROM seller_staff WHERE id = ?", String.class, staffId);
    }

    public void acceptInvite(String token, String password, String name) {
        SellerStaff s = findByToken(token).orElseThrow(() -> ApiException.badRequest("Devet linki etibarsizdir"));
        if (password == null || password.length() < 6) {
            throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        }
        String displayName = name == null || name.isBlank() ? s.name : name.trim();
        String usernameSlug = s.usernameSlug;
        if (usernameSlug == null || usernameSlug.isBlank()) {
            usernameSlug = uniqueStaffSlug(displayName, s.sellerId);
        }
        jdbc.update(
                "UPDATE seller_staff SET password_hash = ?, name = ?, status = 'active', joined_at = ?, "
                        + "invite_token = NULL, invite_expires_at = NULL, username_slug = ? WHERE id = ?",
                encoder.encode(password), displayName, Timestamp.from(Instant.now()), usernameSlug, s.id);
    }

    public void updateStaff(long sellerId, long staffId, String role, Set<String> permissions) {
        SellerStaff s = findById(staffId).orElseThrow(() -> ApiException.notFound("Isci tapilmadi"));
        if (s.sellerId != sellerId) throw ApiException.forbidden("Icazə yoxdur");
        if ("owner".equals(s.role)) throw ApiException.badRequest("Sahib redakte oluna bilmez");
        String safeRole = normalizeRole(role);
        jdbc.update("UPDATE seller_staff SET role = ?, permissions_json = ? WHERE id = ? AND seller_id = ?",
                safeRole,
                Json.write(new ArrayList<>(SellerPermissions.forRole(safeRole,
                        permissions == null ? null : new ArrayList<>(permissions)))),
                staffId, sellerId);
    }

    public void removeStaff(long sellerId, long staffId) {
        SellerStaff s = findById(staffId).orElseThrow(() -> ApiException.notFound("Isci tapilmadi"));
        if (s.sellerId != sellerId) throw ApiException.forbidden("Icazə yoxdur");
        jdbc.update("DELETE FROM seller_staff WHERE id = ? AND seller_id = ?", staffId, sellerId);
    }

    public void setStaffPassword(long staffId, String hash) {
        jdbc.update("UPDATE seller_staff SET password_hash = ? WHERE id = ?", hash, staffId);
    }

    public void setFaceEnrolled(long staffId, String subject) {
        jdbc.update("UPDATE seller_staff SET face_enrolled = 1, face_subject = ? WHERE id = ?", subject, staffId);
    }

    public boolean verifyStaffPassword(long staffId, String password) {
        SellerStaff s = findById(staffId).orElse(null);
        if (s == null || !"active".equals(s.status) || s.passwordHash == null) return false;
        return encoder.matches(password, s.passwordHash);
    }

    public Set<String> permissionsForStaff(SellerStaff staff) {
        if (staff == null) return SellerPermissions.all();
        return SellerPermissions.forRole(staff.role, Json.readStringList(readPermissionsJson(staff.id)));
    }

    private String readPermissionsJson(long staffId) {
        return jdbc.queryForObject("SELECT permissions_json FROM seller_staff WHERE id = ?", String.class, staffId);
    }

    private static String normalizeRole(String role) {
        if (role == null) return "staff";
        return switch (role.trim().toLowerCase()) {
            case "manager", "menecer" -> "manager";
            default -> "staff";
        };
    }

    private static String roleLabel(String role) {
        if ("manager".equals(role)) return "Menecer";
        return "Isci";
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("email", rs.getString("email"));
        m.put("name", rs.getString("name"));
        m.put("role", rs.getString("role"));
        m.put("status", rs.getString("status"));
        m.put("permissions", SellerPermissions.forRole(rs.getString("role"),
                Json.readStringList(rs.getString("permissions_json"))));
        m.put("invited_at", rs.getString("invited_at"));
        m.put("joined_at", rs.getString("joined_at"));
        return m;
    }

    private SellerStaff mapEntity(java.sql.ResultSet rs) throws java.sql.SQLException {
        SellerStaff s = new SellerStaff();
        s.id = rs.getLong("id");
        s.sellerId = rs.getLong("seller_id");
        s.email = rs.getString("email");
        s.name = rs.getString("name");
        s.role = rs.getString("role");
        s.status = rs.getString("status");
        s.passwordHash = rs.getString("password_hash");
        s.permissions = SellerPermissions.forRole(s.role, Json.readStringList(rs.getString("permissions_json")));
        s.inviteToken = rs.getString("invite_token");
        s.invitedAt = rs.getString("invited_at");
        s.joinedAt = rs.getString("joined_at");
        try {
            s.usernameSlug = rs.getString("username_slug");
        } catch (java.sql.SQLException ignored) {
            s.usernameSlug = null;
        }
        try {
            s.faceEnrolled = rs.getInt("face_enrolled") == 1;
        } catch (java.sql.SQLException ignored) {
            s.faceEnrolled = false;
        }
        try {
            s.faceSubject = rs.getString("face_subject");
        } catch (java.sql.SQLException ignored) {
            s.faceSubject = null;
        }
        return s;
    }

    private String uniqueStaffSlug(String name, long sellerId) {
        String base = SlugUtil.personSlug(name);
        if (base.isEmpty()) base = "isci";
        if (!staffSlugExists(base)) return base;
        for (int i = 2; i < 500; i++) {
            String candidate = base + i;
            if (!staffSlugExists(candidate)) return candidate;
        }
        return base + sellerId;
    }

    private boolean staffSlugExists(String slug) {
        Integer owner = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE owner_slug = ?", Integer.class, slug);
        if (owner != null && owner > 0) return true;
        Integer staff = jdbc.queryForObject("SELECT COUNT(*) FROM seller_staff WHERE username_slug = ?", Integer.class, slug);
        return staff != null && staff > 0;
    }
}
