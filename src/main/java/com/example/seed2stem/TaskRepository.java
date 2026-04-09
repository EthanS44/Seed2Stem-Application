package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserCreatedTrue();
    List<Task> findByUserCreatedFalse();
    long countByUserCreatedFalse();
}
