package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // cod | vnpay
    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    // pending | success | failed | refund_requested | refunded
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "vnpay_pay_date", length = 20)
    private String vnpayPayDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Refund fields
    @Column(name = "refund_amount", precision = 15, scale = 0)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_bank_info", length = 255)
    private String refundBankInfo;

    @Column(name = "refund_request_date")
    private LocalDateTime refundRequestDate;

    @Column(name = "refund_approved_at")
    private LocalDateTime refundApprovedAt;

    @Column(name = "refund_approved_by", length = 100)
    private String refundApprovedBy;

    @Column(name = "refund_rejected_at")
    private LocalDateTime refundRejectedAt;

    @Column(name = "refund_rejected_by", length = 100)
    private String refundRejectedBy;

    @Column(name = "refund_reject_reason", length = 500)
    private String refundRejectReason;

    @Column(name = "refund_vnpay_response_code", length = 10)
    private String refundVnpayResponseCode;

    @Column(name = "refund_vnpay_transaction_no", length = 100)
    private String refundVnpayTransactionNo;

    @Column(name = "refund_transfer_proof_url", length = 500)
    private String refundTransferProofUrl;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentRefundImage> refundImages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
