package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VarietyRepository extends JpaRepository<Variety, Long> {
    List<Variety> findAllByOrderByNameAsc();
    Optional<Variety> findByNameIgnoreCase(String name);
    Optional<Variety> findByCode(String code);
}
