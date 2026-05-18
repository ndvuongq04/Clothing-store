package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.clothingstore.clothing_store_be.enums.users.Gender;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 50)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // 0 = Nam, 1 = Nu, 2 = Khac
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "gender", columnDefinition = "TINYINT")
    private Gender gender;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    // "user" hoặc "admin", mặc định "user"
    @Builder.Default
    @Column(name = "role", nullable = false, length = 20)
    private String role = "user";

    // 1 = hoạt động, 0 = bị khoá — mặc định true (1)
    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean status = true;

    // 0 = chưa xác thực, 1 = đã xác thực — mặc định false (0)
    @Builder.Default
    @Column(name = "email_verified", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean emailVerified = false;

    // SHA-256 hash cua refresh token — null khi chua dang nhap
    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    // Token xác thực email (SHA-256 hash)
    @Column(name = "verify_token", length = 255)
    private String verifyToken;

    // Token đặt lại mật khẩu (SHA-256 hash)
    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;

    // Thời điểm hết hạn của reset password token
    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
