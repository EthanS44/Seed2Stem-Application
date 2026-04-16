package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findByStatus(PasswordResetStatus status);
    boolean existsByUsernameAndStatus(String username, PasswordResetStatus status);
    long countByStatus(PasswordResetStatus status);
}
