package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.invoice.InvoiceDetailDto;
import com.clothingstore.clothing_store_be.dto.invoice.InvoiceFilterRequest;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;

public interface InvoiceService {

    // ─── User ─────────────────────────────────────────────

    /**
     * Xem chi tiết hóa đơn theo orderId (user — kiểm tra quyền sở hữu).
     */
    InvoiceDetailDto getInvoiceByOrderId(Long userId, Long orderId);

    /**
     * Tải PDF hóa đơn (chỉ cho phép khi Invoice status = PAID).
     * @return byte[] nội dung file PDF
     */
    byte[] downloadInvoicePdf(Long userId, Long orderId);

    // ─── Admin ────────────────────────────────────────────

    /**
     * Xuất hóa đơn PDF cho đơn hàng (admin — không kiểm tra quyền sở hữu).
     * @return byte[] nội dung file PDF
     */
    byte[] generateInvoicePdf(Long orderId);

    /**
     * Lấy danh sách hóa đơn (admin) — có phân trang, filter, search.
     */
    ResultPaginationDTO getInvoices(InvoiceFilterRequest filter);

    /**
     * Xuất danh sách hóa đơn ra file Excel (phục vụ kế toán).
     */
    byte[] exportInvoicesToExcel(InvoiceFilterRequest filter);

    /**
     * Xem chi tiết 1 hóa đơn (admin).
     */
    InvoiceDetailDto getInvoiceById(Long invoiceId);
}
