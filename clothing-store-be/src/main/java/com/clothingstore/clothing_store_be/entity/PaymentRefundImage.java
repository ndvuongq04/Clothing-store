package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_refund_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
}
