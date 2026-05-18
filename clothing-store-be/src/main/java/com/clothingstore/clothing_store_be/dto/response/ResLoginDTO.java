package com.clothingstore.clothing_store_be.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResLoginDTO {
    private String accessToken;
    private UserInfo user;

    @Getter
    @Setter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String fullName;
        private String role;
    }
}
