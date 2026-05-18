package com.clothingstore.clothing_store_be.enums;

import lombok.Getter;

@Getter
public enum ProductSize {
    // Kích cỡ Áo/Quần (Clothing Sizes)
    XS("XS"),
    S("S"),
    M("M"),
    L("L"),
    XL("XL"),
    XXL("XXL"),

    // Kích cỡ Giày/Dép (Footwear Sizes - EUR)
    SIZE_35("35"),
    SIZE_36("36"),
    SIZE_37("37"),
    SIZE_38("38"),
    SIZE_39("39"),
    SIZE_40("40"),
    SIZE_41("41"),
    SIZE_42("42"),
    SIZE_43("43"),
    SIZE_44("44"),
    SIZE_45("45"),

    // Khác
    FREESIZE("Freesize"),
    OTHER("Khác");

    private final String displayName;

    ProductSize(String displayName) {
        this.displayName = displayName;
    }
}
