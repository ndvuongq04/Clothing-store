package com.clothingstore.clothing_store_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.clothingstore.clothing_store_be.entity.User;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findByVerifyToken(String verifyToken);

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    long countByRoleAndCreatedAtBetween(String role, LocalDateTime start, LocalDateTime end);
}
