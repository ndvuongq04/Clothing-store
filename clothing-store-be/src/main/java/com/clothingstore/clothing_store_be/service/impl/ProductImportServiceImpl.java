package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.product.ImportResultDto;
import com.clothingstore.clothing_store_be.dto.product.ProductCreateRequest;
import com.clothingstore.clothing_store_be.dto.product.VariantCreateRequest;
import com.clothingstore.clothing_store_be.entity.Category;
import com.clothingstore.clothing_store_be.enums.ProductColor;
import com.clothingstore.clothing_store_be.enums.ProductSize;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.CategoryRepository;
import com.clothingstore.clothing_store_be.repository.ProductRepository;
import com.clothingstore.clothing_store_be.service.ProductImportService;
import com.clothingstore.clothing_store_be.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportServiceImpl implements ProductImportService {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // ─── Các cột trong file Excel template ──────────────────
    // A: name | B: categoryId | C: description | D: basePrice
    // E: color | F: size | G: stockQty | H: salePrice | I: importPrice

    private static final String[] HEADERS = {
            "Tên sản phẩm (*)",
            "ID Danh mục (*)",
            "Mô tả",
            "Giá cơ bản (*)",
            "Màu sắc",
            "Kích cỡ",
            "Số lượng tồn kho",
            "Giá bán",
            "Giá nhập"
    };

    // ═══════════════════════════════════════════════════════════
    // 1. Tạo file Excel mẫu
    // ═══════════════════════════════════════════════════════════

    @Override
    public ByteArrayInputStream generateImportTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // ── Sheet 1: Template nhập liệu ──
            XSSFSheet dataSheet = workbook.createSheet("Import Sản phẩm");

            // Tạo style cho header
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle requiredStyle = createRequiredNoteStyle(workbook);
            CellStyle sampleStyle = createSampleStyle(workbook);

            // Dòng header
            Row headerRow = dataSheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                dataSheet.setColumnWidth(i, 5500);
            }

            // Dòng ghi chú
            Row noteRow = dataSheet.createRow(1);
            String[] notes = {
                    "Bắt buộc - Các dòng trùng tên sẽ gộp variant",
                    "Bắt buộc - Xem sheet \"Danh mục\"",
                    "Tùy chọn",
                    "Bắt buộc - VD: 350000",
                    "Xem sheet \"Màu sắc\"",
                    "Xem sheet \"Kích cỡ\"",
                    "Mặc định 0",
                    "Để trống = dùng Giá cơ bản",
                    "Giá nhập/giá vốn"
            };
            for (int i = 0; i < notes.length; i++) {
                Cell cell = noteRow.createCell(i);
                cell.setCellValue(notes[i]);
                cell.setCellStyle(requiredStyle);
            }

            // Dòng dữ liệu mẫu 1 - SP1, variant 1
            Row sample1 = dataSheet.createRow(2);
            fillSampleRow(sample1, sampleStyle,
                    "Áo thun nam basic", "1", "Áo thun cotton thoáng mát", "250000",
                    "Màu đen", "M", "50", "250000", "120000");

            // Dòng dữ liệu mẫu 2 - SP1, variant 2 (cùng tên → gộp variant)
            Row sample2 = dataSheet.createRow(3);
            fillSampleRow(sample2, sampleStyle,
                    "Áo thun nam basic", "1", "Áo thun cotton thoáng mát", "250000",
                    "Màu đen", "L", "30", "250000", "120000");

            // Dòng dữ liệu mẫu 3 - SP2
            Row sample3 = dataSheet.createRow(4);
            fillSampleRow(sample3, sampleStyle,
                    "Quần jean nữ slim fit", "2", "Quần jean co giãn", "450000",
                    "Xanh dương", "S", "20", "420000", "200000");

            // Thêm Data Validation cho cột Color (E) — dropdown từ enum
            addDropdownValidation(dataSheet, workbook, 2, 500, 4, 4,
                    Arrays.stream(ProductColor.values())
                            .map(ProductColor::getDisplayName)
                            .toArray(String[]::new),
                    "Chọn màu sắc");

            // Thêm Data Validation cho cột Size (F) — dropdown từ enum
            addDropdownValidation(dataSheet, workbook, 2, 500, 5, 5,
                    Arrays.stream(ProductSize.values())
                            .map(ProductSize::getDisplayName)
                            .toArray(String[]::new),
                    "Chọn kích cỡ");

            // ── Sheet 2: Danh sách danh mục ──
            XSSFSheet categorySheet = workbook.createSheet("Danh mục");
            CellStyle catHeaderStyle = createHeaderStyle(workbook);

            Row catHeader = categorySheet.createRow(0);
            Cell catIdCell = catHeader.createCell(0);
            catIdCell.setCellValue("ID");
            catIdCell.setCellStyle(catHeaderStyle);
            Cell catNameCell = catHeader.createCell(1);
            catNameCell.setCellValue("Tên danh mục");
            catNameCell.setCellStyle(catHeaderStyle);

            List<Category> categories = categoryRepository.findAll();
            int catRowIdx = 1;
            for (Category cat : categories) {
                Row row = categorySheet.createRow(catRowIdx++);
                row.createCell(0).setCellValue(cat.getId());
                row.createCell(1).setCellValue(cat.getName());
            }
            categorySheet.setColumnWidth(0, 3000);
            categorySheet.setColumnWidth(1, 8000);

            // ── Sheet 3: Danh sách màu sắc ──
            XSSFSheet colorSheet = workbook.createSheet("Màu sắc");
            Row colorHeader = colorSheet.createRow(0);
            Cell colorCodeCell = colorHeader.createCell(0);
            colorCodeCell.setCellValue("Mã");
            colorCodeCell.setCellStyle(catHeaderStyle);
            Cell colorNameCell = colorHeader.createCell(1);
            colorNameCell.setCellValue("Tên hiển thị");
            colorNameCell.setCellStyle(catHeaderStyle);

            int colorRowIdx = 1;
            for (ProductColor color : ProductColor.values()) {
                Row row = colorSheet.createRow(colorRowIdx++);
                row.createCell(0).setCellValue(color.name());
                row.createCell(1).setCellValue(color.getDisplayName());
            }
            colorSheet.setColumnWidth(0, 4000);
            colorSheet.setColumnWidth(1, 5000);

            // ── Sheet 4: Danh sách kích cỡ ──
            XSSFSheet sizeSheet = workbook.createSheet("Kích cỡ");
            Row sizeHeader = sizeSheet.createRow(0);
            Cell sizeCodeCell = sizeHeader.createCell(0);
            sizeCodeCell.setCellValue("Mã");
            sizeCodeCell.setCellStyle(catHeaderStyle);
            Cell sizeNameCell = sizeHeader.createCell(1);
            sizeNameCell.setCellValue("Tên hiển thị");
            sizeNameCell.setCellStyle(catHeaderStyle);

            int sizeRowIdx = 1;
            for (ProductSize size : ProductSize.values()) {
                Row row = sizeSheet.createRow(sizeRowIdx++);
                row.createCell(0).setCellValue(size.name());
                row.createCell(1).setCellValue(size.getDisplayName());
            }
            sizeSheet.setColumnWidth(0, 4000);
            sizeSheet.setColumnWidth(1, 5000);

            // Xuất ra byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw AppException.badRequest("Không thể tạo file template: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. Parse file Excel
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<ProductCreateRequest> parseExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            throw AppException.badRequest("Chỉ hỗ trợ file .xlsx");
        }

        List<ProductCreateRequest> result = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Nhóm các dòng theo tên sản phẩm
            // LinkedHashMap giữ thứ tự insert
            Map<String, ProductCreateRequest> productMap = new LinkedHashMap<>();
            List<String> parseErrors = new ArrayList<>();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) { // bỏ dòng header (0) và ghi chú (1)
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // Bỏ qua dòng trống hoàn toàn
                if (isRowEmpty(row))
                    continue;

                try {
                    String name = getStringValue(row.getCell(0));
                    Long categoryId = getLongValue(row.getCell(1));
                    String description = getStringValue(row.getCell(2));
                    BigDecimal basePrice = getNumericValue(row.getCell(3));

                    String color = getStringValue(row.getCell(4));
                    String size = getStringValue(row.getCell(5));
                    Integer stockQty = getIntValue(row.getCell(6));
                    BigDecimal salePrice = getNumericValue(row.getCell(7));
                    BigDecimal importPrice = getNumericValue(row.getCell(8));

                    if (name == null || name.isBlank()) {
                        parseErrors.add("Dòng " + (i + 1) + ": Tên sản phẩm không được để trống");
                        continue;
                    }
                    if (categoryId == null) {
                        parseErrors.add("Dòng " + (i + 1) + ": ID Danh mục không được để trống");
                        continue;
                    }
                    if (basePrice == null) {
                        parseErrors.add("Dòng " + (i + 1) + ": Giá cơ bản không hợp lệ");
                        continue;
                    }

                    // Nhóm theo name
                    ProductCreateRequest product = productMap.get(name);
                    if (product == null) {
                        product = ProductCreateRequest.builder()
                                .name(name)
                                .categoryId(categoryId)
                                .description(description)
                                .basePrice(basePrice)
                                .status(1)
                                .variants(new ArrayList<>())
                                .imageUrls(new ArrayList<>())
                                .excelRow(i + 1)
                                .build();
                        productMap.put(name, product);
                    }

                    // Thêm variant nếu có color hoặc size
                    if (color != null || size != null) {
                        VariantCreateRequest variant = VariantCreateRequest.builder()
                                .color(color)
                                .size(size)
                                .stockQty(stockQty != null ? stockQty : 0)
                                .salePrice(salePrice)
                                .importPrice(importPrice)
                                .build();
                        product.getVariants().add(variant);
                    }

                } catch (Exception e) {
                    parseErrors.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

            result.addAll(productMap.values());

            if (!parseErrors.isEmpty()) {
                log.warn("Excel parse warnings: {}", parseErrors);
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.badRequest("Lỗi đọc file Excel: " + e.getMessage());
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 3. Import hàng loạt: đọc file → validate → thêm → trả kết quả chi tiết
    // ═══════════════════════════════════════════════════════════

    @Override
    public ImportResultDto bulkImport(MultipartFile file) {
        List<ProductCreateRequest> products = parseExcel(file);

        int success = 0;
        int failed = 0;
        List<String> successProducts = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (products.isEmpty()) {
            return ImportResultDto.builder()
                    .totalRows(0)
                    .success(0)
                    .failed(0)
                    .successProducts(successProducts)
                    .errors(List.of("Không tìm thấy dữ liệu sản phẩm hợp lệ trong file"))
                    .build();
        }

        for (ProductCreateRequest req : products) {
            try {
                // Validate trước khi insert
                List<String> validationErrors = validateProduct(req);
                if (!validationErrors.isEmpty()) {
                    failed++;
                    errors.add("Dòng " + req.getExcelRow() + ": Sản phẩm '" + req.getName() + "': " + String.join("; ", validationErrors));
                    continue;
                }

                productService.create(req, null, null);
                success++;
                int variantCount = (req.getVariants() != null) ? req.getVariants().size() : 0;
                successProducts.add("Dòng " + req.getExcelRow() + ": " + req.getName() + " (" + variantCount + " biến thể)");
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + req.getExcelRow() + ": Sản phẩm '" + req.getName() + "': " + e.getMessage());
            }
        }

        return ImportResultDto.builder()
                .totalRows(products.size())
                .success(success)
                .failed(failed)
                .successProducts(successProducts)
                .errors(errors)
                .build();
    }

    // ─── Validation helpers ─────────────────────────────────

    private List<String> validateProduct(ProductCreateRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getName() == null || req.getName().isBlank()) {
            errors.add("Tên sản phẩm không được để trống");
        } else if (productRepository.existsByNameAndDeletedAtIsNull(req.getName())) {
            errors.add("Sản phẩm '" + req.getName() + "' đã tồn tại trong hệ thống");
        }
        if (req.getCategoryId() == null) {
            errors.add("ID Danh mục không được để trống");
        } else {
            boolean categoryExists = categoryRepository.existsById(req.getCategoryId());
            if (!categoryExists) {
                errors.add("Danh mục với ID=" + req.getCategoryId() + " không tồn tại");
            }
        }
        if (req.getBasePrice() == null || req.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Giá cơ bản phải lớn hơn 0");
        }

        // Validate variant uniqueness (color + size)
        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (VariantCreateRequest v : req.getVariants()) {
                String key = normalize(v.getColor()) + "_" + normalize(v.getSize());
                if (!seen.add(key)) {
                    errors.add("Trùng biến thể: color=" + v.getColor() + ", size=" + v.getSize());
                }
            }
        }

        return errors;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // ─── Excel styling helpers ──────────────────────────────

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createRequiredNoteStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createSampleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void fillSampleRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            cell.setCellStyle(style);
        }
    }

    private void addDropdownValidation(XSSFSheet sheet, XSSFWorkbook workbook,
            int firstRow, int lastRow, int firstCol, int lastCol,
            String[] values, String promptTitle) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper
                .createExplicitListConstraint(values);
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowPromptBox(true);
        validation.createPromptBox(promptTitle, "Chọn từ danh sách hoặc nhập giá trị");
        validation.setShowErrorBox(false); // Cho phép nhập tự do
        sheet.addValidationData(validation);
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getStringValue(cell);
                if (val != null && !val.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    // ─── Cell value helpers ──────────────────────────────

    private String getStringValue(Cell cell) {
        if (cell == null)
            return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String val = cell.getStringCellValue();
                yield (val != null && !val.isBlank()) ? val.trim() : null;
            }
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Long getLongValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isEmpty() ? null : Long.parseLong(val);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal getNumericValue(Cell cell) {
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isEmpty() ? null : new BigDecimal(val);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Integer getIntValue(Cell cell) {
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isEmpty() ? null : Integer.parseInt(val);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
