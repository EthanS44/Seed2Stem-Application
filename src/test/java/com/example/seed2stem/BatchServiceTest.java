package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepo;

    @Mock
    private BatchActivityLogRepository activityLogRepo;

    @Mock
    private WorkerSessionRepository workerSessionRepo;

    @InjectMocks
    private BatchService batchService;

    private User manager;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        manager = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        manager.setId(1L);

        testBatch = new Batch();
        testBatch.setId(1L);
        testBatch.setStrain("Blue Dream");
        testBatch.setStrainAcronym("BLD");
        testBatch.setRoom("B1");
        testBatch.setPlantCount(50);
        testBatch.setStartDate(LocalDate.of(2026, 3, 15));
        testBatch.setStatus(BatchStatus.CLONING);
        testBatch.setBatchCode("26-074-BLD-B1-316-01");
    }

    // --- getAllBatches ---

    @Test
    void getAllBatches_returnsBatchesOrderedByCreatedAt() {
        when(batchRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(testBatch));

        List<Batch> result = batchService.getAllBatches();

        assertEquals(1, result.size());
        assertEquals("Blue Dream", result.get(0).getStrain());
    }

    // --- getBatchById ---

    @Test
    void getBatchById_existingId_returnsBatch() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));

        Batch result = batchService.getBatchById(1L);

        assertNotNull(result);
        assertEquals("Blue Dream", result.getStrain());
    }

    @Test
    void getBatchById_nonExistingId_throwsException() {
        when(batchRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> batchService.getBatchById(999L));
    }

    // --- countActive ---

    @Test
    void countActive_returnsCount() {
        when(batchRepo.countActive()).thenReturn(5L);

        assertEquals(5L, batchService.countActive());
    }

    // --- createBatch ---

    @Test
    void createBatch_validInput_createsBatchAndLog() {
        when(batchRepo.count()).thenReturn(0L);
        when(batchRepo.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.createBatch("Blue Dream", "BLD", "B1",
                50, LocalDate.of(2026, 3, 15), "Test notes", manager);

        assertNotNull(result);
        assertEquals("Blue Dream", result.getStrain());
        assertEquals("BLD", result.getStrainAcronym());
        assertEquals(BatchStatus.CLONING, result.getStatus());
        assertNotNull(result.getBatchCode());
        assertEquals(manager, result.getCreatedBy());

        verify(batchRepo).save(any(Batch.class));
        verify(activityLogRepo).save(any(BatchActivityLog.class));
    }

    @Test
    void createBatch_lowercaseAcronym_convertsToUppercase() {
        when(batchRepo.count()).thenReturn(0L);
        when(batchRepo.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.createBatch("Blue Dream", "bld", "B1",
                50, LocalDate.of(2026, 3, 15), null, manager);

        assertEquals("BLD", result.getStrainAcronym());
    }

    // --- advanceStatus ---

    @Test
    void advanceStatus_fromCloning_goesToVegetative() {
        testBatch.setStatus(BatchStatus.CLONING);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.advanceStatus(1L, manager);

        assertEquals(BatchStatus.VEGETATIVE, result.getStatus());
        verify(activityLogRepo).save(any(BatchActivityLog.class));
    }

    @Test
    void advanceStatus_fromVegetative_goesToFlowering() {
        testBatch.setStatus(BatchStatus.VEGETATIVE);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(BatchStatus.FLOWERING, batchService.advanceStatus(1L, manager).getStatus());
    }

    @Test
    void advanceStatus_fromFlowering_goesToHarvested() {
        testBatch.setStatus(BatchStatus.FLOWERING);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(BatchStatus.HARVESTED, batchService.advanceStatus(1L, manager).getStatus());
    }

    @Test
    void advanceStatus_fromPackaging_goesToPackaged() {
        testBatch.setStatus(BatchStatus.PACKAGING);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(BatchStatus.PACKAGED, batchService.advanceStatus(1L, manager).getStatus());
    }

    @Test
    void advanceStatus_fromPackaged_throwsException() {
        testBatch.setStatus(BatchStatus.PACKAGED);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));

        assertThrows(RuntimeException.class, () -> batchService.advanceStatus(1L, manager));
    }

    @Test
    void advanceStatus_fromDestroyed_throwsException() {
        testBatch.setStatus(BatchStatus.DESTROYED);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));

        assertThrows(RuntimeException.class, () -> batchService.advanceStatus(1L, manager));
    }

    // --- markDestroyed ---

    @Test
    void markDestroyed_anyStatus_setsDestroyed() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.markDestroyed(1L, manager);

        assertEquals(BatchStatus.DESTROYED, result.getStatus());
        verify(activityLogRepo).save(any(BatchActivityLog.class));
    }

    // --- recordHarvest ---

    @Test
    void recordHarvest_floweringBatch_setsHarvestedStatus() {
        testBatch.setStatus(BatchStatus.FLOWERING);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate harvestDate = LocalDate.of(2026, 6, 1);
        Batch result = batchService.recordHarvest(1L, harvestDate, 500.0, 10.0, manager);

        assertEquals(BatchStatus.HARVESTED, result.getStatus());
        assertEquals(harvestDate, result.getHarvestDate());
        assertEquals(500.0, result.getHarvestWeightGrams());
        assertEquals(10.0, result.getCoaSampleWeightGrams());
    }

    @Test
    void recordHarvest_nonFloweringBatch_doesNotChangeStatus() {
        testBatch.setStatus(BatchStatus.HARVESTED);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.recordHarvest(1L, LocalDate.now(), 500.0, null, manager);

        assertEquals(BatchStatus.HARVESTED, result.getStatus()); // stays the same
    }

    @Test
    void recordHarvest_withNullCoaWeight_succeeds() {
        testBatch.setStatus(BatchStatus.FLOWERING);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Batch result = batchService.recordHarvest(1L, LocalDate.now(), 500.0, null, manager);

        assertNull(result.getCoaSampleWeightGrams());
        assertEquals(500.0, result.getHarvestWeightGrams());
    }

    // --- addWorkerSession ---

    @Test
    void addWorkerSession_validInput_createsSession() {
        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(workerSessionRepo.save(any(WorkerSession.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkerSession result = batchService.addWorkerSession(1L, "TRIMMING", "Jane",
                LocalDate.now(), 100.0, 5, "large", 4.0, "Good session", manager);

        assertNotNull(result);
        assertEquals("TRIMMING", result.getSessionType());
        assertEquals("Jane", result.getWorkerName());
        assertEquals(100.0, result.getWeightGrams());
        assertEquals(5, result.getBagCount());
        assertEquals(4.0, result.getHoursWorked());
    }

    // --- Full status progression ---

    @Test
    void advanceStatus_fullProgression_cloningToPackaged() {
        BatchStatus[] expectedOrder = {
                BatchStatus.VEGETATIVE, BatchStatus.FLOWERING, BatchStatus.HARVESTED,
                BatchStatus.DEBUCKING, BatchStatus.TRIMMING, BatchStatus.PACKAGING,
                BatchStatus.PACKAGED
        };

        when(batchRepo.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (BatchStatus expected : expectedOrder) {
            Batch result = batchService.advanceStatus(1L, manager);
            assertEquals(expected, result.getStatus());
        }
    }
}
