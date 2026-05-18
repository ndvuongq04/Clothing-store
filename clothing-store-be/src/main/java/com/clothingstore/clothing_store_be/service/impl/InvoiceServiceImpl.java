package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.invoice.InvoiceDetailDto;
import com.clothingstore.clothing_store_be.dto.invoice.InvoiceDto;
import com.clothingstore.clothing_store_be.dto.invoice.InvoiceFilterRequest;
import com.clothingstore.clothing_store_be.dto.order.OrderItemDto;
import com.clothingstore.clothing_store_be.dto.order.PaymentDto;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.entity.*;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.InvoiceRepository;
import com.clothingstore.clothing_store_be.repository.OrderRepository;
import com.clothingstore.clothing_store_be.service.FileStorageService;
import com.clothingstore.clothing_store_be.service.InvoiceService;
import com.clothingstore.clothing_store_be.specification.InvoiceSpecification;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getInstance(Locale.of("vi", "VN"));
    private static final Color PRIMARY_COLOR = new Color(33, 37, 41);
    private static final Color HEADER_BG = new Color(52, 58, 64);
    private static final Color LIGHT_BG = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(222, 226, 230);

    // ─── Xuất hóa đơn PDF ─────────────────────────────────

    @Override
    public byte[] generateInvoicePdf(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        // Tìm hoặc tạo Invoice
        Invoice invoice = invoiceRepository.findByOrderId(orderId).orElse(null);

        byte[] pdfBytes = buildPdfBytes(order);
        String fileUrl = saveInvoicePdf(order.getOrderCode(), pdfBytes);

        if (invoice == null) {
            invoice = Invoice.builder()
                    .invoiceCode(generateInvoiceCode())
                    .order(order)
                    .status(mapOrderStatusToInvoiceStatus(order))
                    .issuedDate(LocalDate.now())
                    .subtotalAmount(order.getSubTotal())
                    .discountAmount(order.getDiscountAmount())
                    .taxAmount(BigDecimal.ZERO)
                    .totalAmount(order.getTotal())
                    .fileUrl(fileUrl)
                    .build();
        } else {
            // Cập nhật file PDF mới
            if (invoice.getFileUrl() != null) {
                fileStorageService.deleteFile(invoice.getFileUrl());
            }
            invoice.setFileUrl(fileUrl);
            invoice.setStatus(mapOrderStatusToInvoiceStatus(order));
            invoice.setSubtotalAmount(order.getSubTotal());
            invoice.setDiscountAmount(order.getDiscountAmount());
            invoice.setTotalAmount(order.getTotal());
        }
        invoiceRepository.save(invoice);

        return pdfBytes;
    }

    // ─── User: Xem chi tiết hóa đơn ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public InvoiceDetailDto getInvoiceByOrderId(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> AppException.notFound("Chưa có hóa đơn cho đơn hàng này"));

        return toDetailDto(invoice);
    }

    // ─── User: Tải PDF hóa đơn (chỉ khi PAID) ────────────

    @Override
    public byte[] downloadInvoicePdf(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> AppException.notFound("Chưa có hóa đơn cho đơn hàng này"));

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw AppException.badRequest("Hóa đơn chưa được thanh toán. Chỉ có thể tải hóa đơn đã thanh toán.");
        }

        // Generate PDF và cập nhật fileUrl
        byte[] pdfBytes = buildPdfBytes(order);
        String fileUrl = saveInvoicePdf(order.getOrderCode(), pdfBytes);
        invoice.setFileUrl(fileUrl);
        invoiceRepository.save(invoice);

        return pdfBytes;
    }

    // ─── Admin: Danh sách hóa đơn ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getInvoices(InvoiceFilterRequest filter) {
        Sort sort = buildSort(filter.getSortBy());
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getLimit(), sort);

        Page<Invoice> page = invoiceRepository.findAll(
                InvoiceSpecification.buildFilter(filter), pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements());

        List<InvoiceDto> content = page.getContent().stream()
                .map(this::toDto).toList();

        return new ResultPaginationDTO(meta, content);
    }

    // ─── Admin: Chi tiết hóa đơn ──────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InvoiceDetailDto getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn"));
        return toDetailDto(invoice);
    }

    // ─── Admin: Xuất hóa đơn Excel ────────────────────────
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvoicesToExcel(InvoiceFilterRequest filter) {
        Sort sort = buildSort(filter.getSortBy());
        List<Invoice> invoices = invoiceRepository.findAll(InvoiceSpecification.buildFilter(filter), sort);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh_sach_hoa_don");

            // Header Font (Fully qualified to avoid clash with OpenPDF Font)
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Header Style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row for Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Mã HĐ", "Mã Đơn", "Ngày xuất", "Khách hàng", "Email", "Trạng thái", "PT Thanh toán", "Tổng tiền", "Ghi chú"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Data Style (Currency)
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0"));

            int rowIdx = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowIdx++);
                Order order = inv.getOrder();
                User user = order.getUser();

                row.createCell(0).setCellValue(inv.getInvoiceCode() != null ? inv.getInvoiceCode() : "");
                row.createCell(1).setCellValue(order.getOrderCode() != null ? order.getOrderCode() : "");
                row.createCell(2).setCellValue(inv.getIssuedDate() != null ? inv.getIssuedDate().toString() : "");
                row.createCell(3).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(4).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(5).setCellValue(inv.getStatus().name());
                row.createCell(6).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");

                Cell totalCell = row.createCell(7);
                totalCell.setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
                totalCell.setCellStyle(currencyStyle);

                row.createCell(8).setCellValue(inv.getNotes() != null ? inv.getNotes() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw AppException.internalServerError("Lỗi khi tạo file Excel: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Invoice Code Generator
    // ═══════════════════════════════════════════════════════

    private String generateInvoiceCode() {
        // Format: INV-yyyyMMdd-XXXX
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%s-%04d", dateStr, count);
    }

    private InvoiceStatus mapOrderStatusToInvoiceStatus(Order order) {
        if ("cancelled".equals(order.getStatus())) {
            return InvoiceStatus.CANCELLED;
        }
        String ps = order.getPaymentStatus();
        if ("paid".equals(ps)) return InvoiceStatus.PAID;
        if ("refunded".equals(ps)) return InvoiceStatus.REFUNDED;
        return InvoiceStatus.PENDING;
    }

    // ═══════════════════════════════════════════════════════
    //  DTO Mapper
    // ═══════════════════════════════════════════════════════

    private InvoiceDto toDto(Invoice inv) {
        Order order = inv.getOrder();
        return InvoiceDto.builder()
                .invoiceId(inv.getId())
                .invoiceCode(inv.getInvoiceCode())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .status(inv.getStatus().name())
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .issuedDate(inv.getIssuedDate())
                .subtotalAmount(inv.getSubtotalAmount())
                .discountAmount(inv.getDiscountAmount())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .fileUrl(inv.getFileUrl())
                .notes(inv.getNotes())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }

    private InvoiceDetailDto toDetailDto(Invoice inv) {
        Order order = inv.getOrder();
        User user = order.getUser();

        // Customer info
        InvoiceDetailDto.CustomerInfo customerInfo = InvoiceDetailDto.CustomerInfo.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .build();

        // Order items
        List<OrderItemDto> itemDtos = order.getItems() == null ? List.of() :
                order.getItems().stream().map(i -> OrderItemDto.builder()
                        .orderItemId(i.getId())
                        .productId(i.getVariant().getProduct().getId())
                        .variantId(String.valueOf(i.getVariant().getId()))
                        .productName(i.getProductName())
                        .color(i.getColor())
                        .size(i.getSize())
                        .thumbnailUrl(i.getThumbnailUrl())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build()).toList();

        // Payment info
        PaymentDto paymentDto = null;
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentDto = PaymentDto.builder()
                    .paymentId(p.getId())
                    .method(p.getMethod())
                    .status(p.getStatus())
                    .amount(p.getAmount())
                    .vnpayTransactionNo(p.getVnpayTransactionNo())
                    .paidAt(p.getPaidAt())
                    .build();
        }

        // Order info
        InvoiceDetailDto.OrderInfo orderInfo = InvoiceDetailDto.OrderInfo.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getVoucherCode() : null)
                .note(order.getNote())
                .trackingCode(order.getTrackingCode())
                .recipientName(order.getShipFullName())
                .recipientPhone(order.getShipPhone())
                .shipStreet(order.getShipStreet())
                .shipWard(order.getShipWard())
                .shipDistrict(order.getShipDistrict())
                .shipProvince(order.getShipProvince())
                .payment(paymentDto)
                .items(itemDtos)
                .orderCreatedAt(order.getCreatedAt())
                .build();

        return InvoiceDetailDto.builder()
                .invoiceId(inv.getId())
                .invoiceCode(inv.getInvoiceCode())
                .status(inv.getStatus().name())
                .issuedDate(inv.getIssuedDate())
                .subtotalAmount(inv.getSubtotalAmount())
                .discountAmount(inv.getDiscountAmount())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .fileUrl(inv.getFileUrl())
                .notes(inv.getNotes())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .customer(customerInfo)
                .order(orderInfo)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    //  PDF Generation
    // ═══════════════════════════════════════════════════════

    private byte[] buildPdfBytes(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // ─── Fonts (Unicode) ──────────────────────────
            BaseFont bf = BaseFont.createFont("fonts/Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfBold = BaseFont.createFont("fonts/Roboto-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            Font titleFont = new Font(bfBold, 22, Font.BOLD, PRIMARY_COLOR);
            Font headerFont = new Font(bfBold, 10, Font.BOLD, Color.WHITE);
            Font labelFont = new Font(bfBold, 10, Font.BOLD, PRIMARY_COLOR);
            Font valueFont = new Font(bf, 10, Font.NORMAL, PRIMARY_COLOR);
            Font smallFont = new Font(bf, 9, Font.NORMAL, Color.GRAY);
            Font totalLabelFont = new Font(bfBold, 11, Font.BOLD, PRIMARY_COLOR);
            Font totalValueFont = new Font(bfBold, 14, Font.BOLD, new Color(220, 53, 69));

            // ─── Header ───────────────────────────────────
            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph storeName = new Paragraph("CLOTHING STORE", new Font(bfBold, 12, Font.BOLD, Color.GRAY));
            storeName.setAlignment(Element.ALIGN_CENTER);
            storeName.setSpacingAfter(5);
            document.add(storeName);

            // Divider
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingAfter(15);
            PdfPCell divCell = new PdfPCell();
            divCell.setBorderWidthBottom(2);
            divCell.setBorderColorBottom(HEADER_BG);
            divCell.setBorderWidthTop(0);
            divCell.setBorderWidthLeft(0);
            divCell.setBorderWidthRight(0);
            divCell.setFixedHeight(5);
            divider.addCell(divCell);
            document.add(divider);

            // ─── Order info ───────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 1});
            infoTable.setSpacingAfter(15);

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(0);
            leftCell.addElement(new Phrase("Mã đơn hàng: ", labelFont));
            leftCell.addElement(new Phrase(order.getOrderCode(), valueFont));
            leftCell.addElement(new Phrase("\nNgày đặt: ", labelFont));
            leftCell.addElement(new Phrase(order.getCreatedAt().format(DATE_FMT), valueFont));
            leftCell.addElement(new Phrase("\nPhương thức TT: ", labelFont));
            leftCell.addElement(new Phrase(formatPaymentMethod(order.getPaymentMethod()), valueFont));
            leftCell.addElement(new Phrase("\nTrạng thái TT: ", labelFont));
            leftCell.addElement(new Phrase(formatPaymentStatus(order.getPaymentStatus()), valueFont));
            infoTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(0);
            rightCell.addElement(new Phrase("Người nhận: ", labelFont));
            rightCell.addElement(new Phrase(order.getShipFullName(), valueFont));
            rightCell.addElement(new Phrase("\nSĐT: ", labelFont));
            rightCell.addElement(new Phrase(order.getShipPhone() != null ? order.getShipPhone() : "—", valueFont));
            rightCell.addElement(new Phrase("\nĐịa chỉ: ", labelFont));
            rightCell.addElement(new Phrase(buildAddress(order), valueFont));
            infoTable.addCell(rightCell);

            document.add(infoTable);

            // ─── Items table ──────────────────────────────
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 3f, 1.2f, 1f, 1.5f});
            table.setSpacingAfter(10);

            addHeaderCell(table, "STT", headerFont);
            addHeaderCell(table, "Sản phẩm", headerFont);
            addHeaderCell(table, "Đơn giá", headerFont);
            addHeaderCell(table, "SL", headerFont);
            addHeaderCell(table, "Thành tiền", headerFont);

            int index = 1;
            for (OrderItem item : order.getItems()) {
                Color rowBg = (index % 2 == 0) ? LIGHT_BG : Color.WHITE;

                addDataCell(table, String.valueOf(index), valueFont, Element.ALIGN_CENTER, rowBg);

                PdfPCell productCell = new PdfPCell();
                productCell.setPadding(6);
                productCell.setBorderColor(BORDER_COLOR);
                productCell.setBackgroundColor(rowBg);
                productCell.addElement(new Phrase(item.getProductName(), valueFont));
                if (item.getColor() != null || item.getSize() != null) {
                    StringBuilder detail = new StringBuilder();
                    if (item.getColor() != null) detail.append("Màu: ").append(item.getColor());
                    if (item.getSize() != null) {
                        if (!detail.isEmpty()) detail.append(" | ");
                        detail.append("Size: ").append(item.getSize());
                    }
                    productCell.addElement(new Phrase(detail.toString(), smallFont));
                }
                table.addCell(productCell);

                addDataCell(table, formatCurrency(item.getUnitPrice()), valueFont, Element.ALIGN_RIGHT, rowBg);
                addDataCell(table, String.valueOf(item.getQuantity()), valueFont, Element.ALIGN_CENTER, rowBg);
                addDataCell(table, formatCurrency(item.getLineTotal()), valueFont, Element.ALIGN_RIGHT, rowBg);
                index++;
            }

            document.add(table);

            // ─── Totals ───────────────────────────────────
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(50);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setWidths(new float[]{1.5f, 1.5f});

            addTotalRow(totalTable, "Tạm tính:", formatCurrency(order.getSubTotal()), labelFont, valueFont);

            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                String discountText = "- " + formatCurrency(order.getDiscountAmount());
                if (order.getVoucher() != null) {
                    discountText += " (" + order.getVoucher().getVoucherCode() + ")";
                }
                addTotalRow(totalTable, "Giảm giá:", discountText, labelFont, valueFont);
            }

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TỔNG CỘNG:", totalLabelFont));
            totalLabelCell.setBorder(0);
            totalLabelCell.setPaddingTop(8);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(totalLabelCell);

            PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(order.getTotal()) + " đ", totalValueFont));
            totalValueCell.setBorder(0);
            totalValueCell.setPaddingTop(8);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(totalValueCell);

            document.add(totalTable);

            // ─── Note ─────────────────────────────────────
            if (order.getNote() != null && !order.getNote().isBlank()) {
                document.add(Chunk.NEWLINE);
                Paragraph noteLabel = new Paragraph("Ghi chú:", labelFont);
                noteLabel.setSpacingBefore(10);
                document.add(noteLabel);
                document.add(new Paragraph(order.getNote(), valueFont));
            }

            // ─── Footer ──────────────────────────────────
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Cảm ơn quý khách đã mua hàng!", new Font(bfBold, 11, Font.ITALIC, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            Paragraph printDate = new Paragraph(
                    "Ngày xuất: " + LocalDateTime.now().format(DATE_FMT), smallFont);
            printDate.setAlignment(Element.ALIGN_CENTER);
            printDate.setSpacingBefore(5);
            document.add(printDate);

            document.close();
            return baos.toByteArray();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo hóa đơn PDF: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════

    private String saveInvoicePdf(String orderCode, byte[] pdfBytes) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("uploads", "invoices").toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            String filename = "INV-" + orderCode + ".pdf";
            java.nio.file.Path dest = dir.resolve(filename);
            java.nio.file.Files.write(dest, pdfBytes);
            return "/uploads/invoices/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu file hóa đơn: " + e.getMessage(), e);
        }
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "amount_asc" -> Sort.by(Sort.Direction.ASC, "totalAmount");
            case "amount_desc" -> Sort.by(Sort.Direction.DESC, "totalAmount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(bg);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(0);
        lCell.setPaddingTop(4);
        lCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(0);
        vCell.setPaddingTop(4);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vCell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return CURRENCY_FMT.format(amount);
    }

    private String buildAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getShipStreet() != null) sb.append(order.getShipStreet());
        if (order.getShipWard() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(order.getShipWard());
        }
        if (order.getShipDistrict() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(order.getShipDistrict());
        }
        if (order.getShipProvince() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(order.getShipProvince());
        }
        return sb.toString();
    }

    private String formatPaymentMethod(String method) {
        if (method == null) return "—";
        return switch (method) {
            case "cod" -> "Thanh toán khi nhận hàng (COD)";
            case "vnpay" -> "Thanh toán qua VNPay";
            default -> method;
        };
    }

    private String formatPaymentStatus(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "unpaid" -> "Chưa thanh toán";
            case "paid" -> "Đã thanh toán";
            case "refund_requested" -> "Yêu cầu hoàn tiền";
            case "refunded" -> "Đã hoàn tiền";
            default -> status;
        };
    }
}
