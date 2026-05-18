package com.clothingstore.clothing_store_be.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.clothingstore.clothing_store_be.dto.request.ReqLoginDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqRegisterDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqForgotPasswordDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqResetPasswordDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqChangePasswordDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqResendVerifyDTO;
import com.clothingstore.clothing_store_be.dto.request.ReqUpdateProfileDTO;
import com.clothingstore.clothing_store_be.dto.response.ResLoginDTO;
import com.clothingstore.clothing_store_be.dto.response.ResUserProfileDTO;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.security.CustomUserDetails;
import com.clothingstore.clothing_store_be.security.JwtService;
import com.clothingstore.clothing_store_be.service.UserService;
import com.clothingstore.clothing_store_be.exception.AppException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final UserService userService;
        private final JwtService jwtService;
        private final JwtDecoder jwtDecoder;

        @Value("${app.jwt.refresh-token-expiration}")
        private long refreshTokenExpiration;

        @Value("${app.cookie.secure}")
        private boolean cookieSecure;

        @PostMapping("/login")
        public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO request) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword());

                Authentication authentication = authenticationManager.authenticate(authToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                String accessToken = jwtService.generateAccessToken(
                                userDetails.getId(),
                                userDetails.getEmail(),
                                userDetails.getRole());
                String refreshToken = jwtService.generateRefreshToken(userDetails.getId());

                // Luu hash cua refresh token vao cot refresh_token trong bang users
                userService.saveRefreshToken(userDetails.getId(), refreshToken);

                ResponseCookie resCookie = ResponseCookie
                                .from("refresh_token", refreshToken)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/api/v1/auth/refresh")
                                .maxAge(refreshTokenExpiration / 1000)
                                .build();

                ResLoginDTO resLoginDTO = ResLoginDTO.builder()
                                .accessToken(accessToken)
                                .user(ResLoginDTO.UserInfo.builder()
                                                .id(userDetails.getId())
                                                .email(userDetails.getEmail())
                                                .fullName(userDetails.getFullName())
                                                .role(userDetails.getRole())
                                                .build())
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookie.toString())
                                .body(resLoginDTO);
        }

        @PostMapping("/register")
        public ResponseEntity<String> register(@Valid @RequestBody ReqRegisterDTO request) {
                userService.registerCustomer(request);
                return ResponseEntity.ok("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.");
        }

        @PostMapping("/refresh")
        public ResponseEntity<ResLoginDTO> refresh(
                        @CookieValue(name = "refresh_token", required = false) String rawRefreshToken) {

                if (rawRefreshToken == null) {
                        throw AppException.unauthorized("Khong tim thay refresh token");
                }

                // Kiem tra chu ky va han su dung cua refresh token
                try {
                        jwtDecoder.decode(rawRefreshToken);
                } catch (JwtException e) {
                        throw AppException.unauthorized("Refresh token khong hop le hoac da het han");
                }

                // Tim user theo hash, kiem tra con hoat dong khong
                User user = userService.findActiveUserByRefreshToken(rawRefreshToken);

                // Tao tokens moi (rotation — moi lan refresh la doi refresh token moi)
                String newAccessToken = jwtService.generateAccessToken(
                                user.getUserId(), user.getEmail(), user.getRole());
                String newRefreshToken = jwtService.generateRefreshToken(user.getUserId());

                userService.saveRefreshToken(user.getUserId(), newRefreshToken);

                ResponseCookie resCookie = ResponseCookie
                                .from("refresh_token", newRefreshToken)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/api/v1/auth/refresh")
                                .maxAge(refreshTokenExpiration / 1000)
                                .build();

                ResLoginDTO resLoginDTO = ResLoginDTO.builder()
                                .accessToken(newAccessToken)
                                .user(ResLoginDTO.UserInfo.builder()
                                                .id(user.getUserId())
                                                .email(user.getEmail())
                                                .fullName(user.getFullName())
                                                .role(user.getRole())
                                                .build())
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookie.toString())
                                .body(resLoginDTO);
        }

        @PostMapping("/logout")
        public ResponseEntity<String> logout(@AuthenticationPrincipal Jwt jwt) {
                Long userId = Long.parseLong(jwt.getSubject());
                userService.logout(userId);

                // Xoa cookie refresh_token phia client
                ResponseCookie deleteCookie = ResponseCookie
                                .from("refresh_token", "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/api/v1/auth/refresh")
                                .maxAge(0)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                                .body("Dang xuat thanh cong");
        }

        // ========================= XÁC THỰC EMAIL =========================

        @GetMapping("/verify-email")
        public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
                userService.verifyEmail(token);
                return ResponseEntity.ok("Xác thực email thành công. Bạn có thể đăng nhập.");
        }

        @PostMapping("/resend-verify")
        public ResponseEntity<String> resendVerifyEmail(@Valid @RequestBody ReqResendVerifyDTO request) {
                userService.resendVerifyEmail(request.getEmail());
                return ResponseEntity.ok("Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.");
        }

        // ========================= QUÊN / ĐẶT LẠI MẬT KHẨU =========================

        @PostMapping("/forgot-password")
        public ResponseEntity<String> forgotPassword(@Valid @RequestBody ReqForgotPasswordDTO request) {
                userService.forgotPassword(request.getEmail());
                return ResponseEntity.ok("Đã gửi email hướng dẫn đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.");
        }

        @PostMapping("/reset-password")
        public ResponseEntity<String> resetPassword(@Valid @RequestBody ReqResetPasswordDTO request) {
                userService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới.");
        }

        @PostMapping("/change-password")
        public ResponseEntity<String> changePassword(@Valid @RequestBody ReqChangePasswordDTO request,
                        @AuthenticationPrincipal Jwt jwt) {
                Long userId = Long.parseLong(jwt.getSubject());
                userService.changePassword(userId, request);
                return ResponseEntity.ok("Đổi mật khẩu thành công.");
        }

        @GetMapping("/me")
        public ResponseEntity<ResUserProfileDTO> getProfile(@AuthenticationPrincipal Jwt jwt) {
                Long userId = Long.parseLong(jwt.getSubject());
                return ResponseEntity.ok(userService.getProfile(userId));
        }

        @PutMapping("/me")
        public ResponseEntity<ResUserProfileDTO> updateProfile(
                        @Valid @RequestBody ReqUpdateProfileDTO request,
                        @AuthenticationPrincipal Jwt jwt) {
                Long userId = Long.parseLong(jwt.getSubject());
                return ResponseEntity.ok(userService.updateProfile(userId, request));
        }
}
