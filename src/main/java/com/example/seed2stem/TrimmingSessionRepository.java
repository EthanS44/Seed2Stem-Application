package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrimmingSessionRepository extends JpaRepository<TrimmingSession, Long> {
    List<TrimmingSession> findByBatchOrderBySessionDateAsc(Batch batch);
    
    @Query("SELECT SUM(w.weightTrimmedInGrams) FROM TrimmingWorkerEntry w " +
           "WHERE w.trimmingSession.batch = :batch")
    Double calculateTotalWeightTrimmed(Batch batch);
}
