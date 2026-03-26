package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchActivityLogRepository extends JpaRepository<BatchActivityLog, Long> {

    List<BatchActivityLog> findByBatchIdOrderByTimestampDesc(Long batchId);

    @Query("SELECT l FROM BatchActivityLog l JOIN FETCH l.batch WHERE l.timestamp BETWEEN :start AND :end")
    List<BatchActivityLog> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
