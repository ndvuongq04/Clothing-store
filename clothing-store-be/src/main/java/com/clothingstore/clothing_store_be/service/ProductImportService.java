package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.product.ImportResultDto;
import com.clothingstore.clothing_store_be.dto.product.ProductCreateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface ProductImportService {

    List<ProductCreateRequest> parseExcel(MultipartFile file);

    ImportResultDto bulkImport(MultipartFile file);

    ByteArrayInputStream generateImportTemplate();
}
