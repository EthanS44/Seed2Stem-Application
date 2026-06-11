package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ---- existing (now interpreted as "everything, deleted or not") ----
    List<Task> findByUserCreatedTrue();
    List<Task> findByUserCreatedFalse();
    List<Task> findByUserCreatedFalseOrderByTitleAsc();
    long countByUserCreatedFalse();

    // ---- soft-delete-aware finders ----
    /** Active (non-archived) standard tasks, alphabetized. */
    List<Task> findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc();

    /** Archived standard tasks, alphabetized. */
    List<Task> findByUserCreatedFalseAndDeletedTrueOrderByTitleAsc();

    /** Active (non-archived) user-created tasks. */
    List<Task> findByUserCreatedTrueAndDeletedFalse();

    /** Archived user-created tasks. */
    List<Task> findByUserCreatedTrueAndDeletedTrue();
}
