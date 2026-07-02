package com.bizdevar.home;

import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stories")
public class AdminHomeStoryController {

    private final HomeStoryRepository stories;
    private final HomeStoryImageService images;
    private final JdbcTemplate jdbc;

    public AdminHomeStoryController(HomeStoryRepository stories, HomeStoryImageService images, JdbcTemplate jdbc) {
        this.stories = stories;
        this.images = images;
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest r) {
        guard(r);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", stories.listAll());
        return out;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(HttpServletRequest r, @RequestParam("file") MultipartFile file) {
        guard(r);
        return Map.of("url", images.saveImage(file));
    }

    @PostMapping
    public Map<String, Object> create(HttpServletRequest r, @RequestBody StoryBody body) {
        AppUser a = guard(r);
        validate(body, true);
        long id = stories.insert(
                trim(body.title),
                trim(body.image_url),
                trim(body.link_url),
                body.sort_order == null ? 0 : body.sort_order,
                body.is_active == null || body.is_active);
        audit(a, r, "story_create", "story:" + id);
        return Map.of("ok", true, "id", id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(HttpServletRequest r, @PathVariable long id, @RequestBody StoryBody body) {
        AppUser a = guard(r);
        validate(body, false);
        stories.update(
                id,
                trim(body.title),
                trim(body.image_url),
                trim(body.link_url),
                body.sort_order == null ? 0 : body.sort_order,
                body.is_active == null || body.is_active);
        audit(a, r, "story_update", "story:" + id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        stories.delete(id);
        audit(a, r, "story_delete", "story:" + id);
        return Map.of("ok", true);
    }

    private void validate(StoryBody body, boolean requireImage) {
        if (body == null) throw com.bizdevar.common.ApiException.badRequest("Melumat daxil edin");
        if (body.title == null || body.title.isBlank()) {
            throw com.bizdevar.common.ApiException.badRequest("Basliq daxil edin");
        }
        if (requireImage && (body.image_url == null || body.image_url.isBlank())) {
            throw com.bizdevar.common.ApiException.badRequest("Sekil teleb olunur");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private AppUser guard(HttpServletRequest r) {
        return AuthSupport.requireAdminLegacy(r);
    }

    private void audit(AppUser admin, HttpServletRequest r, String event, String resource) {
        try {
            jdbc.update("INSERT INTO audit_logs (event_type, admin_id, ip, resource) VALUES (?, ?, ?, ?)",
                    event, admin.id, r.getRemoteAddr(), resource);
        } catch (Exception ignored) {
        }
    }

    public static class StoryBody {
        public String title;
        public String image_url;
        public String link_url;
        public Integer sort_order;
        public Boolean is_active;
    }
}
