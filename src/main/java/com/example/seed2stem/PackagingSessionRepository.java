package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackagingSessionRepository extends JpaRepository<PackagingSession, Long> {
    List<PackagingSession> findByBatchOrderBySessionDateAsc(Batch batch);
    
    @Query("SELECT SUM(e.bagSizeInGrams * e.numberOfBags + COALESCE(e.partialBagWeightInGrams, 0)) " +
           "FROM PackagingEntry e WHERE e.packagingSession.batch = :batch")
    Double calculateTotalWeightPackaged(Batch batch);
}
