package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrimmingWorkerEntryRepository extends JpaRepository<TrimmingWorkerEntry, Long> {
    List<TrimmingWorkerEntry> findByWorker(User worker);
    
    @Query("SELECT w FROM TrimmingWorkerEntry w " +
           "WHERE w.trimmingSession.batch = :batch")
    List<TrimmingWorkerEntry> findByBatch(Batch batch);
    
    @Query("SELECT w.worker, SUM(w.weightTrimmedInGrams) " +
           "FROM TrimmingWorkerEntry w " +
           "WHERE w.trimmingSession.batch = :batch " +
           "GROUP BY w.worker")
    List<Object[]> calculateWorkerPerformanceByBatch(Batch batch);
}
