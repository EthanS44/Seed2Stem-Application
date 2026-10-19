package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByBatchCode(String batchCode);
    List<Batch> findAllByOrderByStartDateDescIdDesc();
    List<Batch> findByRoomOrderByStartDateDescIdDesc(Room room);
}
