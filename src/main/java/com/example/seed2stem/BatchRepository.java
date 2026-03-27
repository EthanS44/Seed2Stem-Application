package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    
    Optional<Batch> findByBatchCode(String batchCode);
    
    // Get the highest crop number for a given year to auto-increment
    @Query("SELECT MAX(b.cropNumber) FROM Batch b WHERE b.year = :year")
    Integer findMaxCropNumberForYear(Integer year);
}
