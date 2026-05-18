package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.request.ReqRegisterDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqUpdateProfileDTO;
import com.clothingstore.clothing_store_be.dto.response.ResUserProfileDTO;
import com.clothingstore.clothing_store_be.entity.User;

public interface UserService {
    void registerCustomer(ReqRegisterDTO request);

    void saveRefreshToken(Long userId, String rawRefreshToken);

    void logout(Long userId);

    // Tim user theo hash cua refresh token, kiem tra status con hoat dong khong
    User findActiveUserByRefreshToken(String rawRefreshToken);

    // Xác thực email qua token
    void verifyEmail(String token);

    // Gửi lại email xác thực
    void resendVerifyEmail(String email);

    // Quên mật khẩu — gửi email chứa link đặt lại mật khẩu
    void forgotPassword(String email);

    // Đặt lại mật khẩu bằng token
    void resetPassword(String token, String newPassword);

    // Đổi mật khẩu cho người dùng đang đăng nhập
    void changePassword(Long userId, com.clothingstore.clothing_store_be.dto.request.ReqChangePasswordDTO request);

    ResUserProfileDTO getProfile(Long userId);

    ResUserProfileDTO updateProfile(Long userId, ReqUpdateProfileDTO request);
}
