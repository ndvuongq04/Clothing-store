package com.clothingstore.clothing_store_be.dto.address;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateRequest {

    private String fullName;

    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String province;
    private String district;
    private String ward;
    private String street;
}
