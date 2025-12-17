package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistRunRepository
        extends JpaRepository<ChecklistRun, Long> {
    List<ChecklistRun> findByStatus(ChecklistRunStatus status);
}

