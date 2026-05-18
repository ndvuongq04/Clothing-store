package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSetting {

    // cod | vnpay
    @Id
    @Column(name = "method", length = 20)
    private String method;

    @Builder.Default
    @Column(name = "is_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
