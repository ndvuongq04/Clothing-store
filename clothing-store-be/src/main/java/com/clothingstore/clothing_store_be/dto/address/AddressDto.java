package com.clothingstore.clothing_store_be.dto.address;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {
    private Long id;
    private String fullName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String street;
    private boolean isDefault;
}
