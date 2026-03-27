package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BatchService {

    private final BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    /**
     * Creates a new batch with auto-generated crop number and batch code
     */
    @Transactional
    public Batch createBatch(Integer year, Integer dayOfYear, String strainName,
                            String strainAcronym, String destinationRoom, Integer numberOfClones) {
        
        // Get the next crop number for this year
        Integer maxCropNumber = batchRepository.findMaxCropNumberForYear(year);
        Integer nextCropNumber = (maxCropNumber == null) ? 1 : maxCropNumber + 1;

        // Create the batch (batch code is auto-generated in constructor)
        Batch batch = new Batch(year, dayOfYear, strainName, strainAcronym, 
                               destinationRoom, nextCropNumber);
        batch.setNumberOfClones(numberOfClones);
        batch.setCreatedDate(LocalDate.now());
        batch.setStatus(BatchStatus.CLONING);

        return batchRepository.save(batch);
    }

    /**
     * Helper method to get current year as two digits
     */
    public static Integer getCurrentYearTwoDigits() {
        return LocalDate.now().getYear() % 100;
    }

    /**
     * Helper method to get day of year
     */
    public static Integer getCurrentDayOfYear() {
        return LocalDate.now().getDayOfYear();
    }

    /**
     * Find batch by batch code
     */
    public Batch findByBatchCode(String batchCode) {
        return batchRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchCode));
    }

    /**
     * Update batch status
     */
    @Transactional
    public Batch updateBatchStatus(String batchCode, BatchStatus newStatus) {
        Batch batch = findByBatchCode(batchCode);
        batch.setStatus(newStatus);
        return batchRepository.save(batch);
    }
}
