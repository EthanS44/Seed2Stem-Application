package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriggerRecordRepository extends JpaRepository<TriggerRecord, Long> {
    List<TriggerRecord> findByBatch(Batch batch);
}
