package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HarvestRecordRepository extends JpaRepository<HarvestRecord, Long> {
    List<HarvestRecord> findByBatch(Batch batch);
    Optional<HarvestRecord> findByBatchAndChecklistRunNotNull(Batch batch);
}
