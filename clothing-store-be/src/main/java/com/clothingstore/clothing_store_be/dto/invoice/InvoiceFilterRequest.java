package com.clothingstore.clothing_store_be.dto.invoice;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFilterRequest {

    private String keyword;          // tìm theo mã hóa đơn, mã đơn hàng, tên KH, email

    private String status;           // PENDING / PAID / REFUNDED / CANCELLED (InvoiceStatus)

    private String paymentMethod;    // cod / vnpay

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private int page = 0;
    private int limit = 20;
    private String sortBy;           // newest, oldest, amount_asc, amount_desc
}
