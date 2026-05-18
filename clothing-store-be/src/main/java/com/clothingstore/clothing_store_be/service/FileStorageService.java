package com.clothingstore.clothing_store_be.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    /**
     * Upload nhiều file vào thư mục con (subfolder).
     * @return danh sách relative URL (dùng để lưu DB & trả về client)
     */
    List<String> uploadFiles(String subfolder, List<MultipartFile> files);

    /**
     * Xóa 1 file theo relative URL đã lưu.
     */
    void deleteFile(String fileUrl);
}
