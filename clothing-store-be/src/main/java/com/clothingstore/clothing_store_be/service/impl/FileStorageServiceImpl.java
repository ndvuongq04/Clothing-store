package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload: " + rootLocation, e);
        }
    }

    @Override
    public List<String> uploadFiles(String subfolder, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        Path targetDir = rootLocation.resolve(subfolder).normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw AppException.badRequest("Không thể tạo thư mục lưu file");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validateImage(file);
            String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
            try {
                Path dest = targetDir.resolve(filename).normalize();
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                // Trả về relative URL: /uploads/subfolder/filename
                urls.add("/uploads/" + subfolder + "/" + filename);
            } catch (IOException e) {
                throw AppException.badRequest("Upload file thất bại: " + file.getOriginalFilename());
            }
        }
        return urls;
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            // fileUrl = "/uploads/reviews/xxx.jpg" → resolve "reviews/xxx.jpg"
            String relativePath = fileUrl.replaceFirst("^/uploads/", "");
            Path filePath = rootLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning nhưng không throw — xóa ảnh thất bại không nên block flow
        }
    }

    // ─── Helpers ──────────────────────────────────────────

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("File rỗng");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw AppException.badRequest("Chỉ chấp nhận file ảnh (image/*)");
        }
        // Giới hạn 5MB mỗi ảnh
        if (file.getSize() > 5 * 1024 * 1024) {
            throw AppException.badRequest("Kích thước ảnh tối đa 5MB");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".jpg";
    }
}
