package com.clothingstore.clothing_store_be.enums;

import lombok.Getter;

@Getter
public enum ProductColor {
    BLACK("Màu đen"),
    WHITE("Màu trắng"),
    RED("Màu đỏ"),
    BLUE("Xanh dương"),
    GREEN("Xanh lá"),
    YELLOW("Màu vàng"),
    PINK("Màu hồng"),
    PURPLE("Màu tím"),
    GRAY("Màu xám"),
    BROWN("Màu nâu"),
    ORANGE("Màu cam"),
    BEIGE("Màu be"),
    NAVY("Xanh navy"),
    MULTI("Nhiều màu"),
    OTHER("Màu khác");

    private final String displayName;

    ProductColor(String displayName) {
        this.displayName = displayName;
    }
}
