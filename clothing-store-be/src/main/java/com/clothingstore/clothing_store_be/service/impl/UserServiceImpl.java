package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.request.ReqChangePasswordDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqRegisterDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqUpdateProfileDTO;
import com.clothingstore.clothing_store_be.dto.response.ResUserProfileDTO;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.UserRepository;
import com.clothingstore.clothing_store_be.service.EmailService;
import com.clothingstore.clothing_store_be.service.UserService;
import com.clothingstore.clothing_store_be.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HashUtil hashUtil;
    private final EmailService emailService;

    @Value("${app.verify-url:http://localhost:5173/verify-email?token=}")
    private String verifyUrlBase;

    @Value("${app.reset-password-url:http://localhost:5173/reset-password?token=}")
    private String resetPasswordUrlBase;

    // ========================= ĐĂNG KÝ =========================

    @Override
    public void registerCustomer(ReqRegisterDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email da duoc su dung");
        }

        String rawToken = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("user")
                .status(true)
                .emailVerified(false)
                .verifyToken(hashUtil.hashSHA256(rawToken))
                .build();

        userRepository.save(user);

        // Gửi email xác thực
        String verifyUrl = verifyUrlBase + rawToken;
        emailService.sendVerifyEmail(user.getEmail(), user.getFullName(), verifyUrl);
    }

    // ========================= XÁC THỰC EMAIL =========================

    @Override
    public void verifyEmail(String token) {
        String hashedToken = hashUtil.hashSHA256(token);
        User user = userRepository.findByVerifyToken(hashedToken)
                .orElseThrow(() -> AppException.badRequest("Token xác thực không hợp lệ hoặc đã được sử dụng"));

        user.setEmailVerified(true);
        user.setVerifyToken(null); // Xóa token sau khi xác thực
        userRepository.save(user);
    }

    @Override
    public void resendVerifyEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Email không tồn tại trong hệ thống"));

        if (user.isEmailVerified()) {
            throw AppException.badRequest("Tài khoản đã được xác thực");
        }

        String rawToken = UUID.randomUUID().toString();
        user.setVerifyToken(hashUtil.hashSHA256(rawToken));
        userRepository.save(user);

        String verifyUrl = verifyUrlBase + rawToken;
        emailService.sendVerifyEmail(user.getEmail(), user.getFullName(), verifyUrl);
    }

    // ========================= QUÊN / ĐẶT LẠI MẬT KHẨU =========================

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Email không tồn tại trong hệ thống"));

        String rawToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(hashUtil.hashSHA256(rawToken));
        user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String resetUrl = resetPasswordUrlBase + rawToken;
        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), resetUrl);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String hashedToken = hashUtil.hashSHA256(token);
        User user = userRepository.findByResetPasswordToken(hashedToken)
                .orElseThrow(() -> AppException.badRequest("Token đặt lại mật khẩu không hợp lệ"));

        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu mới");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userRepository.save(user);
    }

    // ========================= REFRESH TOKEN & LOGOUT =========================

    @Override
    public void changePassword(Long userId, ReqChangePasswordDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User khong ton tai"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw AppException.badRequest("Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public ResUserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User khong ton tai"));
        return toUserProfileDto(user);
    }

    @Override
    public ResUserProfileDTO updateProfile(Long userId, ReqUpdateProfileDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User khong ton tai"));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(normalizePhoneNumber(request.getPhoneNumber()));
        user.setDateOfBirth(request.getDateOfBirth());

        return toUserProfileDto(userRepository.save(user));
    }

    @Override
    public User findActiveUserByRefreshToken(String rawRefreshToken) {
        String hash = hashUtil.hashSHA256(rawRefreshToken);
        User user = userRepository.findByRefreshToken(hash)
                .orElseThrow(() -> AppException.unauthorized("Refresh token khong hop le"));
        if (!user.isStatus()) {
            throw AppException.unauthorized("Tai khoan bi khoa");
        }
        return user;
    }

    @Override
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User khong ton tai"));
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Override
    public void saveRefreshToken(Long userId, String rawRefreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User khong ton tai"));
        user.setRefreshToken(hashUtil.hashSHA256(rawRefreshToken));
        userRepository.save(user);
    }

    private ResUserProfileDTO toUserProfileDto(User user) {
        return ResUserProfileDTO.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole())
                .build();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String trimmedPhoneNumber = phoneNumber.trim();
        return trimmedPhoneNumber.isEmpty() ? null : trimmedPhoneNumber;
    }
}
