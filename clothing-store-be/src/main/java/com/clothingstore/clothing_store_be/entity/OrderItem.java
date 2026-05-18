package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    // Snapshot tại thời điểm đặt hàng
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "size", length = 10)
    private String size;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 0)
    private BigDecimal lineTotal;

    // Giá vốn tại thời điểm mua (để tính lợi nhuận)
    @Column(name = "import_price", precision = 15, scale = 0)
    private BigDecimal importPrice;
}
