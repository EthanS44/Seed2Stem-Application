package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkerSessionRepository extends JpaRepository<WorkerSession, Long> {

    List<WorkerSession> findByBatchIdOrderBySessionDateDesc(Long batchId);

    List<WorkerSession> findByBatchIdAndSessionType(Long batchId, String sessionType);

    @Query("SELECT w FROM WorkerSession w JOIN FETCH w.batch WHERE w.sessionDate BETWEEN :start AND :end")
    List<WorkerSession> findBySessionDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
