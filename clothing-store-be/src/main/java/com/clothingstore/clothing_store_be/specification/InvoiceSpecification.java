package com.clothingstore.clothing_store_be.specification;

import com.clothingstore.clothing_store_be.dto.invoice.InvoiceFilterRequest;
import com.clothingstore.clothing_store_be.entity.Invoice;
import com.clothingstore.clothing_store_be.entity.Order;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> buildFilter(InvoiceFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Invoice, Order> orderJoin = root.join("order", JoinType.INNER);

            // Keyword: tìm theo mã hóa đơn, mã đơn hàng, tên KH, email KH
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Join<Order, User> userJoin = orderJoin.join("user", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceCode")), kw),
                        cb.like(cb.lower(orderJoin.get("orderCode")), kw),
                        cb.like(cb.lower(userJoin.get("fullName")), kw),
                        cb.like(cb.lower(userJoin.get("email")), kw)
                ));
            }

            // Filter theo InvoiceStatus (PENDING / PAID / REFUNDED / CANCELLED)
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                try {
                    InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(filter.getStatus().trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), invoiceStatus));
                } catch (IllegalArgumentException ignored) {
                    // Bỏ qua nếu status không hợp lệ
                }
            }

            // Filter theo payment method (cod / vnpay)
            if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
                predicates.add(cb.equal(orderJoin.get("paymentMethod"), filter.getPaymentMethod().trim()));
            }

            // Filter theo khoảng ngày xuất hóa đơn
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issuedDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issuedDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
