package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChecklistRunRepository
        extends JpaRepository<ChecklistRun, Long> {
    List<ChecklistRun> findByStatus(ChecklistRunStatus status);
    List<ChecklistRun> findByCompletedByAndStatus(User completedBy, ChecklistRunStatus status);
    Optional<ChecklistRun> findByCompletedByAndTaskAndStatus(User completedBy, Task task, ChecklistRunStatus status);

    @Query("SELECT r FROM ChecklistRun r JOIN FETCH r.completedBy WHERE r.startTime BETWEEN :start AND :end")
    List<ChecklistRun> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r FROM ChecklistRun r JOIN FETCH r.task WHERE r.completedBy = :user AND r.status IN ('APPROVED', 'PENDING') ORDER BY r.startTime DESC")
    List<ChecklistRun> findCompletedByUserOrderByStartTimeDesc(@Param("user") User user);
}

