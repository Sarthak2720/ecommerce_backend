package com.styliste.repository;

import com.styliste.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByEmailAndOtp(String email, String otp);
    Optional<PasswordReset> findTopByEmailOrderByExpiryTimeDesc(String email);
    void deleteByEmail(String email); // To clean up after successful reset
}