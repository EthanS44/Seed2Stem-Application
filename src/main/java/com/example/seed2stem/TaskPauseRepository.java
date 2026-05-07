package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskPauseRepository extends JpaRepository<TaskPause, Long> {
    List<TaskPause> findByChecklistRunOrderByStartTimeAsc(ChecklistRun run);
    Optional<TaskPause> findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(ChecklistRun run);
    List<TaskPause> findByChecklistRunInAndStartTimeBetween(
            List<ChecklistRun> runs, LocalDateTime from, LocalDateTime to);

    @Query("SELECT p.checklistRun.id FROM TaskPause p WHERE p.endTime IS NULL")
    List<Long> findRunIdsWithOpenPause();
}
