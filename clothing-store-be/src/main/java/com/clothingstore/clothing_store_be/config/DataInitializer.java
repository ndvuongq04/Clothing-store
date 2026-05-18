package com.clothingstore.clothing_store_be.config;

import java.time.LocalDate;
import java.util.List;

import com.clothingstore.clothing_store_be.entity.PaymentSetting;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.enums.users.Gender;
import com.clothingstore.clothing_store_be.repository.PaymentSettingRepository;
import com.clothingstore.clothing_store_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentSettingRepository paymentSettingRepository;

    @Override
    public void run(String... args) {
        createSuperAdminIfNotExists();
        createPaymentSettingsIfNotExists();
    }

    private void createSuperAdminIfNotExists() {
        String email = "superadmin@clothingstore.vn";

        if (userRepository.existsByEmail(email)) {
            log.info("Super admin da ton tai, bo qua khoi tao");
            return;
        }

        User superAdmin = User.builder()
                .fullName("Super Admin")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.NAM)
                .email(email)
                .password(passwordEncoder.encode("S123456"))
                .phoneNumber("0000000000")
                .role("admin")
                .status(true)
                .emailVerified(true)
                .build();

        userRepository.save(superAdmin);
        log.info("Da tao tai khoan Super Admin: {}", email);
    }

    private void createPaymentSettingsIfNotExists() {
        List<String> methods = List.of("cod", "vnpay");
        for (String method : methods) {
            if (!paymentSettingRepository.existsById(method)) {
                paymentSettingRepository.save(PaymentSetting.builder()
                        .method(method)
                        .isEnabled(true)
                        .build());
                log.info("Da tao payment setting: {}", method);
            }
        }
    }
}
