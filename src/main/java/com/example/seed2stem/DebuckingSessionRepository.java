package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebuckingSessionRepository extends JpaRepository<DebuckingSession, Long> {
    List<DebuckingSession> findByBatchOrderBySessionDateAsc(Batch batch);
    
    @Query("SELECT SUM(d.weightDebuckedInGrams) FROM DebuckingSession d WHERE d.batch = :batch")
    Double calculateTotalWeightDebucked(Batch batch);
}
