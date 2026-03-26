package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batchRepo;
    private final BatchActivityLogRepository activityLogRepo;
    private final WorkerSessionRepository workerSessionRepo;

    public BatchService(BatchRepository batchRepo,
                        BatchActivityLogRepository activityLogRepo,
                        WorkerSessionRepository workerSessionRepo) {
        this.batchRepo = batchRepo;
        this.activityLogRepo = activityLogRepo;
        this.workerSessionRepo = workerSessionRepo;
    }

    public List<Batch> getAllBatches() {
        return batchRepo.findAllByOrderByCreatedAtDesc();
    }

    public Batch getBatchById(Long id) {
        return batchRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
    }

    public long countActive() {
        return batchRepo.countActive();
    }

    public List<BatchActivityLog> getActivityLog(Long batchId) {
        return activityLogRepo.findByBatchIdOrderByTimestampDesc(batchId);
    }

    public List<WorkerSession> getWorkerSessions(Long batchId) {
        return workerSessionRepo.findByBatchIdOrderBySessionDateDesc(batchId);
    }

    @Transactional
    public Batch createBatch(String strain, String strainAcronym, String room,
                             Integer plantCount, LocalDate startDate, String notes, User createdBy) {
        Batch batch = new Batch();
        batch.setStrain(strain);
        batch.setStrainAcronym(strainAcronym.toUpperCase());
        batch.setRoom(room);
        batch.setPlantCount(plantCount);
        batch.setStartDate(startDate);
        batch.setNotes(notes);
        batch.setCreatedBy(createdBy);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setStatus(BatchStatus.CLONING);
        batch.setBatchCode(generateBatchCode(batch));

        batch = batchRepo.save(batch);

        activityLogRepo.save(new BatchActivityLog(
                batch, null, BatchStatus.CLONING, createdBy, "Batch created"));

        return batch;
    }

    @Transactional
    public Batch advanceStatus(Long batchId, User performedBy) {
        Batch batch = getBatchById(batchId);
        BatchStatus current = batch.getStatus();

        BatchStatus next = switch (current) {
            case CLONING -> BatchStatus.VEGETATIVE;
            case VEGETATIVE -> BatchStatus.FLOWERING;
            case FLOWERING -> BatchStatus.HARVESTED;
            case HARVESTED -> BatchStatus.DEBUCKING;
            case DEBUCKING -> BatchStatus.TRIMMING;
            case TRIMMING -> BatchStatus.PACKAGING;
            case PACKAGING -> BatchStatus.PACKAGED;
            default -> throw new RuntimeException("Cannot advance from status: " + current.getDisplayName());
        };

        batch.setStatus(next);
        batch = batchRepo.save(batch);

        activityLogRepo.save(new BatchActivityLog(
                batch, current, next, performedBy,
                "Status advanced from " + current.getDisplayName() + " to " + next.getDisplayName()));

        return batch;
    }

    @Transactional
    public Batch markDestroyed(Long batchId, User performedBy) {
        Batch batch = getBatchById(batchId);
        BatchStatus previous = batch.getStatus();
        batch.setStatus(BatchStatus.DESTROYED);
        batch = batchRepo.save(batch);

        activityLogRepo.save(new BatchActivityLog(
                batch, previous, BatchStatus.DESTROYED, performedBy, "Batch marked as destroyed"));

        return batch;
    }

    @Transactional
    public Batch recordHarvest(Long batchId, LocalDate harvestDate, Double weight,
                               Double coaWeight, User performedBy) {
        Batch batch = getBatchById(batchId);
        batch.setHarvestDate(harvestDate);
        batch.setHarvestWeightGrams(weight);
        batch.setCoaSampleWeightGrams(coaWeight);

        BatchStatus previous = batch.getStatus();
        if (batch.getStatus() == BatchStatus.FLOWERING) {
            batch.setStatus(BatchStatus.HARVESTED);
        }
        batch = batchRepo.save(batch);

        activityLogRepo.save(new BatchActivityLog(
                batch, previous, batch.getStatus(), performedBy,
                "Harvest recorded: " + weight + "g" + (coaWeight != null ? ", CoA: " + coaWeight + "g" : "")));

        return batch;
    }

    @Transactional
    public WorkerSession addWorkerSession(Long batchId, String sessionType, String workerName,
                                           LocalDate sessionDate, Double weightGrams,
                                           Integer bagCount, String bagSize,
                                           Double hoursWorked, String notes, User recordedBy) {
        Batch batch = getBatchById(batchId);

        WorkerSession session = new WorkerSession();
        session.setBatch(batch);
        session.setSessionType(sessionType);
        session.setWorkerName(workerName);
        session.setSessionDate(sessionDate);
        session.setWeightGrams(weightGrams);
        session.setBagCount(bagCount);
        session.setBagSize(bagSize);
        session.setHoursWorked(hoursWorked);
        session.setNotes(notes);
        session.setRecordedBy(recordedBy);
        session.setCreatedAt(LocalDateTime.now());

        return workerSessionRepo.save(session);
    }

    private String generateBatchCode(Batch batch) {
        LocalDate date = batch.getStartDate();
        String yy = String.format("%02d", date.getYear() % 100);
        String ddd = String.format("%03d", date.getDayOfYear());
        String aaa = batch.getStrainAcronym().length() >= 3
                ? batch.getStrainAcronym().substring(0, 3)
                : String.format("%-3s", batch.getStrainAcronym()).replace(' ', 'X');
        String rr = batch.getRoom();

        long count = batchRepo.count() + 1;
        String cc = String.format("%02d", count);

        return yy + "-" + ddd + "-" + aaa + "-" + rr + "-316-" + cc;
    }
}
