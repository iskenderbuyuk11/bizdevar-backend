package com.bizdevar.home;

import com.bizdevar.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class HomeStoryImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final java.util.List<String> ALLOWED = java.util.List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path uploadDir;

    public HomeStoryImageService() {
        Path base = Paths.get(System.getProperty("user.dir"));
        if (base.getFileName() != null && "backend".equalsIgnoreCase(base.getFileName().toString())) {
            base = base.getParent();
        }
        this.uploadDir = base.resolve("uploads").resolve("stories");
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Sekil secilmeyib");
        }
        validate(file);
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw ApiException.badRequest("Sekil qovluğu yaradıla bilmədi");
        }
        String ext = extension(file);
        String name = UUID.randomUUID() + ext;
        try {
            file.transferTo(uploadDir.resolve(name).toFile());
        } catch (IOException e) {
            throw ApiException.badRequest("Sekil yuklenmedi");
        }
        return "uploads/stories/" + name;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Sekil cox boyukdur (max 5MB)");
        }
        String type = file.getContentType();
        if (type == null || ALLOWED.stream().noneMatch(t -> t.equalsIgnoreCase(type))) {
            throw ApiException.badRequest("Yalniz JPG, PNG, WEBP, GIF qebul edilir");
        }
    }

    private String extension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(jpe?g|png|webp|gif)")) return ext;
        }
        String type = file.getContentType();
        if ("image/png".equals(type)) return ".png";
        if ("image/webp".equals(type)) return ".webp";
        if ("image/gif".equals(type)) return ".gif";
        return ".jpg";
    }
}
