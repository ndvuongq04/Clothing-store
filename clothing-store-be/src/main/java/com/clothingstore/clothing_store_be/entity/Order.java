package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    // Snapshot địa chỉ tại thời điểm đặt hàng
    @Column(name = "ship_full_name", length = 100)
    private String shipFullName;

    @Column(name = "ship_phone", length = 15)
    private String shipPhone;

    @Column(name = "ship_province", length = 100)
    private String shipProvince;

    @Column(name = "ship_district", length = 100)
    private String shipDistrict;

    @Column(name = "ship_ward", length = 100)
    private String shipWard;

    @Column(name = "ship_street", length = 255)
    private String shipStreet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_code")
    private Voucher voucher;

    // pending | confirmed | shipping | completed | cancelled | payment_failed |
    // recjected_refund | refund_requested | refunded
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "pending";

    // cod | vnpay
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    // unpaid | paid | refund_requested | refunded
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "unpaid";

    @Column(name = "sub_total", nullable = false, precision = 15, scale = 0)
    private BigDecimal subTotal;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 0)
    private BigDecimal total;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    // 'user' | 'admin'
    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.orderCode == null) {
            String ts = now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
            int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
            this.orderCode = "ORD" + ts + rand;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
