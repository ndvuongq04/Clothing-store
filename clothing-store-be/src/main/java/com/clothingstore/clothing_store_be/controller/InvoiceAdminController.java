package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.invoice.InvoiceDetailDto;
import com.clothingstore.clothing_store_be.dto.invoice.InvoiceFilterRequest;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/invoices")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService invoiceService;

    /**
     * GET /admin/invoices — danh sách hóa đơn có phân trang + filter
     * Query params: keyword, status, paymentMethod, fromDate, toDate, page, limit, sortBy
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getInvoices(@ModelAttribute InvoiceFilterRequest filter) {
        return ResponseEntity.ok(invoiceService.getInvoices(filter));
    }

    /**
     * GET /admin/invoices/{id} — chi tiết 1 hóa đơn
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailDto> getInvoiceById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /**
     * GET /admin/invoices/order/{orderId}/pdf — xuất PDF hóa đơn theo orderId
     */
    @GetMapping("/order/{orderId}/pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("orderId") Long orderId) {
        byte[] pdf = invoiceService.generateInvoicePdf(orderId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-order-" + orderId + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
    /**
     * GET /admin/invoices/export/excel — Xuất danh sách hóa đơn ra Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute InvoiceFilterRequest filter) {
        byte[] excelBytes = invoiceService.exportInvoicesToExcel(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "invoices.xlsx");
        
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
