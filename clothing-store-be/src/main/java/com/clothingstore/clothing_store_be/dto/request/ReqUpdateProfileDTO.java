package com.clothingstore.clothing_store_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReqUpdateProfileDTO {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 50, message = "Họ tên không vượt quá 50 ký tự")
    private String fullName;

    private String phoneNumber;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;
}
