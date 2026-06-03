package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    List<RegistrationRequest> findByStatus(RegistrationStatus status);
    boolean existsByEmail(String email);
}
