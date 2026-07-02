package com.bizdevar.seller;

import com.bizdevar.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path uploadDir;

    public ProductImageService() {
        Path base = Paths.get(System.getProperty("user.dir"));
        if (base.getFileName() != null && "backend".equalsIgnoreCase(base.getFileName().toString())) {
            base = base.getParent();
        }
        this.uploadDir = base.resolve("uploads").resolve("products");
    }

    public List<String> saveImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw ApiException.badRequest("Sekil secilmeyib");
        }
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw ApiException.badRequest("Sekil qovluğu yaradıla bilmədi");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            validate(file);
            String ext = extension(file);
            String name = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(name);
            try {
                file.transferTo(target.toFile());
            } catch (IOException e) {
                throw ApiException.badRequest("Sekil yuklenmedi: " + file.getOriginalFilename());
            }
            urls.add("uploads/products/" + name);
        }
        if (urls.isEmpty()) {
            throw ApiException.badRequest("Sekil secilmeyib");
        }
        return urls;
    }

    public String saveStoreLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Sekil secilmeyib");
        }
        Path vendorDir = uploadDir.getParent().resolve("vendors");
        try {
            Files.createDirectories(vendorDir);
        } catch (IOException e) {
            throw ApiException.badRequest("Sekil qovluğu yaradıla bilmədi");
        }
        validate(file);
        String ext = extension(file);
        String name = UUID.randomUUID() + ext;
        Path target = vendorDir.resolve(name);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw ApiException.badRequest("Profil sekli yuklenmedi");
        }
        return "uploads/vendors/" + name;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Sekil cox boyukdur (max 5MB): " + file.getOriginalFilename());
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
