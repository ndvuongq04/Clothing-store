package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants", uniqueConstraints = @UniqueConstraint(name = "uk_variant_product_color_size", columnNames = {
        "product_id", "color", "size" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "size", length = 10)
    private String size;

    @Builder.Default
    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty = 0;

    // override price per variant (nullable)
    @Column(name = "sale_price", precision = 15, scale = 0)
    private BigDecimal salePrice;

    // Giá vốn (để tính lợi nhuận)
    @Column(name = "import_price", precision = 15, scale = 0)
    private BigDecimal importPrice;

    @Column(name = "sku", length = 100)
    private String sku;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
