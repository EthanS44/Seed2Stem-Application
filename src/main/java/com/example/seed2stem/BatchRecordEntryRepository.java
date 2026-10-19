package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BatchRecordEntryRepository extends JpaRepository<BatchRecordEntry, Long> {
    Optional<BatchRecordEntry> findByRun(ChecklistRun run);
    Optional<BatchRecordEntry> findByRunId(Long runId);

    /** Every non-draft line for a batch, oldest first (the record's running log). */
    List<BatchRecordEntry> findByBatchAndStatusInOrderByEntryDateAscRecordedAtAscIdAsc(
            Batch batch, Collection<BatchEntryStatus> statuses);

    List<BatchRecordEntry> findByBatchAndStatus(Batch batch, BatchEntryStatus status);

    /** Pending seeds/cloning entries are attached to a batch number, not a batch, until approval. */
    List<BatchRecordEntry> findByProposedBatchCodeAndBatchIsNullAndStatus(String code, BatchEntryStatus status);

    List<BatchRecordEntry> findByActionAndRoomAndStatusOrderByEntryDateDescRecordedAtDesc(
            BatchAction action, Room room, BatchEntryStatus status);
}
