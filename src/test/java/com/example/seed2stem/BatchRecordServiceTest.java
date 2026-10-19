package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchRecordServiceTest {

    @Mock private BatchRepository batchRepo;
    @Mock private BatchRecordEntryRepository entryRepo;
    @Mock private VarietyRepository varietyRepo;
    @Mock private ChecklistRunRepository runRepo;
    @Mock private ChecklistRunService runService;

    @InjectMocks
    private BatchRecordService service;

    private User tech;
    private User manager;
    private ChecklistRun run;
    private Variety blueDream;

    @BeforeEach
    void setUp() {
        tech = new User("tech", "h", "Tech", "One", AccountType.TECHNICIAN);
        tech.setId(1L);
        manager = new User("mgr", "h", "Manager", "One", AccountType.MANAGER);
        manager.setId(2L);
        run = new ChecklistRun();
        run.setId(10L);
        run.setCompletedBy(tech);
        run.setStartTime(LocalDateTime.of(2026, 10, 8, 9, 0));
        run.setStatus(ChecklistRunStatus.IN_PROGRESS);
        blueDream = new Variety("Blue Dream", "BLD");
        blueDream.setId(5L);
    }

    private BatchRecordEntry entry(BatchAction action) {
        BatchRecordEntry e = new BatchRecordEntry();
        e.setId(100L);
        e.setRun(run);
        e.setAction(action);
        e.setTechnician(tech);
        e.setEntryDate(LocalDate.of(2026, 10, 8));
        e.setRoom(Room.B1);
        e.setStatus(BatchEntryStatus.PENDING);
        return e;
    }

    private Batch batch(BatchSource source) {
        Batch b = new Batch();
        b.setId(50L);
        b.setBatchCode("261008-BLD-B1");
        b.setVariety(blueDream);
        b.setRoom(Room.B1);
        b.setSource(source);
        b.setStartDate(LocalDate.of(2026, 10, 8));
        return b;
    }

    private static BatchRecordEntry approved(BatchAction action, Integer plants, Double weight) {
        BatchRecordEntry e = new BatchRecordEntry();
        e.setId((long) (Math.random() * 1_000_000) + 1000);
        e.setAction(action);
        e.setStatus(BatchEntryStatus.APPROVED);
        e.setEntryDate(LocalDate.of(2026, 10, 8));
        e.setRoom(Room.B1);
        e.setPlantCount(plants);
        e.setWeightKg(weight);
        return e;
    }

    // --- batch numbers ---

    @Test
    void buildBatchCode_usesYymmddVarietyRoom() {
        assertEquals("261008-ABC-B1", BatchRecordService.buildBatchCode(LocalDate.of(2026, 10, 8), "ABC", Room.B1));
    }

    // --- totals and loss ---

    @Test
    void totals_lossIsWetMinusEverythingWeighedAfter() {
        BatchRecordEntry debuck = approved(BatchAction.DEBUCK_DESTEM, 10, null);
        debuck.setFanLeafKg(2.0);
        debuck.setStemKg(1.0);
        BatchRecordEntry trim = approved(BatchAction.TRIMMING, null, null);
        trim.setTrimKg(0.5);
        BatchTotals t = BatchTotals.of(List.of(
                approved(BatchAction.HARVEST, 10, 10.0), debuck, trim, approved(BatchAction.DRY_WEIGHT, null, 1.5)));

        assertEquals(5.0, t.getLossKg(), 1e-9);
        assertEquals(50.0, t.getLossPercent(), 1e-9);
        assertEquals("Dried", t.getStage());
    }

    @Test
    void totals_noDryWeight_lossIsNull() {
        BatchTotals t = BatchTotals.of(List.of(approved(BatchAction.HARVEST, 10, 10.0)));
        assertNull(t.getLossKg());
        assertEquals("Harvested", t.getStage());
    }

    @Test
    void totals_onHandSubtractsDestroyedMaterial() {
        BatchRecordEntry debuck = approved(BatchAction.DEBUCK_DESTEM, 10, null);
        debuck.setStemKg(3.0);
        debuck.setFanLeafKg(1.0);
        BatchRecordEntry destroyed = approved(BatchAction.DESTRUCTION, null, 1.25);
        destroyed.setMaterial(DestructionMaterial.STEMS);

        BatchTotals t = BatchTotals.of(List.of(debuck, destroyed));

        assertEquals(1.75, t.getOnHandKg(DestructionMaterial.STEMS), 1e-9);
        assertEquals(1.0, t.getOnHandKg(DestructionMaterial.FAN_LEAVES), 1e-9);
    }

    // --- filter status ---

    @Test
    void filterStatus_dueAfterSevenDays_overdueAfterFourteen() {
        LocalDate today = LocalDate.of(2026, 10, 20);
        assertEquals("OK", new FilterStatus(Room.B1, today.minusDays(7), today).getState());
        assertEquals("DUE", new FilterStatus(Room.B1, today.minusDays(8), today).getState());
        assertEquals("OVERDUE", new FilterStatus(Room.B1, today.minusDays(15), today).getState());
        assertEquals("NONE", new FilterStatus(Room.B1, null, today).getState());
    }

    // --- draft parsing ---

    @Test
    void saveDraft_newVariety_uppercasesCodeAndRoundsKg() {
        Task task = new Task();
        task.setBatchAction(BatchAction.CLONING);
        when(entryRepo.findByRun(run)).thenReturn(Optional.empty());
        when(entryRepo.save(any(BatchRecordEntry.class))).thenAnswer(i -> i.getArgument(0));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("br_room", "B2");
        params.add("br_varietyId", BatchRecordService.NEW_VARIETY);
        params.add("br_newVarietyName", " Blue Dream ");
        params.add("br_newVarietyCode", "bld");
        params.add("br_plantCount", "40");
        params.add("br_weightKg", "1.23456");

        BatchRecordEntry e = service.saveDraft(task, run, params);

        assertEquals(BatchEntryStatus.DRAFT, e.getStatus());
        assertEquals("Blue Dream", e.getNewVarietyName());
        assertEquals("BLD", e.getNewVarietyCode());
        assertEquals("261008-BLD-B2", e.getProposedBatchCode());
        assertEquals(40, e.getPlantCount());
        assertEquals(1.235, e.getWeightKg(), 1e-9);
    }

    // --- submit validation ---

    @Test
    void submit_cloning_valid_setsPendingAndSubmitsRun() {
        BatchRecordEntry e = entry(BatchAction.CLONING);
        e.setStatus(BatchEntryStatus.DRAFT);
        e.setVariety(blueDream);
        e.setSource("Mother #3");
        e.setPlantCount(40);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));
        when(batchRepo.findByBatchCode("261008-BLD-B1")).thenReturn(Optional.empty());

        service.submitRun(10L, List.of());

        assertEquals(BatchEntryStatus.PENDING, e.getStatus());
        assertEquals("261008-BLD-B1", e.getProposedBatchCode());
        verify(runService).submitWithResponses(10L, List.of());
    }

    @Test
    void submit_newVarietyCodeWithDigits_isRejected() {
        BatchRecordEntry e = entry(BatchAction.CLONING);
        e.setNewVarietyName("Gelato");
        e.setNewVarietyCode("G1");
        e.setSource("Mother");
        e.setPlantCount(5);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("2 or 3 letters"));
        verify(runService, never()).submitWithResponses(any(), any());
    }

    @Test
    void submit_newVarietyCodeTakenByAnotherVariety_isRejected() {
        BatchRecordEntry e = entry(BatchAction.RECEIVE_SEEDS);
        e.setNewVarietyName("Blueberry");
        e.setNewVarietyCode("BLD");
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));
        when(varietyRepo.findByNameIgnoreCase("Blueberry")).thenReturn(Optional.empty());
        when(varietyRepo.findByCode("BLD")).thenReturn(Optional.of(blueDream));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("already used by Blue Dream"));
    }

    @Test
    void submit_filterBelowMerv8_isRejected() {
        BatchRecordEntry e = entry(BatchAction.FILTER_CHANGE);
        e.setBatch(batch(BatchSource.CLONES));
        e.setFilterWork(FilterWork.CHANGED);
        e.setFilterCount(4);
        e.setFilterRating(7);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("MERV 8"));
    }

    @Test
    void submit_batchInOtherRoom_isRejected() {
        BatchRecordEntry e = entry(BatchAction.HARVEST);
        e.setRoom(Room.B2);
        e.setBatch(batch(BatchSource.CLONES));
        e.setPlantCount(1);
        e.setWeightKg(1.0);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("isn't in room B2"));
    }

    @Test
    void submit_transferMorePlantsThanLeft_countsOtherPendingTransfers() {
        Batch b = batch(BatchSource.CLONES);
        BatchRecordEntry e = entry(BatchAction.TRANSFER_TO_FLOWER);
        e.setBatch(b);
        e.setPlantCount(15);
        BatchRecordEntry otherPending = entry(BatchAction.TRANSFER_TO_FLOWER);
        otherPending.setId(101L);
        otherPending.setPlantCount(30);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));
        when(entryRepo.findByBatchAndStatus(b, BatchEntryStatus.APPROVED))
                .thenReturn(List.of(approved(BatchAction.CLONING, 40, null)));
        when(entryRepo.findByBatchAndStatus(b, BatchEntryStatus.PENDING)).thenReturn(List.of(e, otherPending));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("Only 10 plants"));
    }

    @Test
    void submit_destroyMoreStemsThanOnHand_isRejected() {
        Batch b = batch(BatchSource.CLONES);
        BatchRecordEntry e = entry(BatchAction.DESTRUCTION);
        e.setBatch(b);
        e.setMaterial(DestructionMaterial.STEMS);
        e.setWeightKg(2.5);
        e.setDestructionMethod("Grind and mix with soil");
        e.setWitness("Pat Lee");
        BatchRecordEntry debuck = approved(BatchAction.DEBUCK_DESTEM, 10, null);
        debuck.setStemKg(2.0);
        debuck.setFanLeafKg(0.0);
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));
        when(entryRepo.findByBatchAndStatus(b, BatchEntryStatus.APPROVED)).thenReturn(List.of(debuck));
        when(entryRepo.findByBatchAndStatus(b, BatchEntryStatus.PENDING)).thenReturn(List.of(e));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertTrue(ex.getMessage().contains("Only 2.000 kg of stems"));
    }

    @Test
    void submit_destructionWithoutWitness_isRejected() {
        BatchRecordEntry e = entry(BatchAction.DESTRUCTION);
        e.setBatch(batch(BatchSource.CLONES));
        e.setMaterial(DestructionMaterial.TRIM);
        e.setWeightKg(0.5);
        e.setDestructionMethod("Compost");
        when(runRepo.findById(10L)).thenReturn(Optional.of(run));
        when(entryRepo.findByRun(run)).thenReturn(Optional.of(e));

        BatchRecordException ex = assertThrows(BatchRecordException.class, () -> service.submitRun(10L, List.of()));
        assertEquals("Enter the witness.", ex.getMessage());
    }

    // --- approval ---

    @Test
    void approve_firstCloningEntry_createsBatch() {
        BatchRecordEntry e = entry(BatchAction.CLONING);
        e.setVariety(blueDream);
        e.setPlantCount(40);
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));
        when(batchRepo.findByBatchCode("261008-BLD-B1")).thenReturn(Optional.empty());
        when(batchRepo.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));

        service.approveRun(10L, manager, "ok");

        verify(batchRepo).save(argThat(b -> b.getBatchCode().equals("261008-BLD-B1")
                && b.getSource() == BatchSource.CLONES && b.getRoom() == Room.B1));
        assertEquals(BatchEntryStatus.APPROVED, e.getStatus());
        assertNotNull(e.getBatch());
        verify(runService).authorizeByManager(10L, manager, "ok");
    }

    @Test
    void approve_laterCloningSameDay_joinsExistingBatch() {
        Batch existing = batch(BatchSource.CLONES);
        BatchRecordEntry e = entry(BatchAction.CLONING);
        e.setVariety(blueDream);
        e.setPlantCount(12);
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));
        when(batchRepo.findByBatchCode("261008-BLD-B1")).thenReturn(Optional.of(existing));

        service.approveRun(10L, manager, "ok");

        verify(batchRepo, never()).save(any());
        assertSame(existing, e.getBatch());
    }

    @Test
    void approve_seedsWhereCloneBatchHasSameNumber_isBlocked() {
        BatchRecordEntry e = entry(BatchAction.RECEIVE_SEEDS);
        e.setVariety(blueDream);
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));
        when(batchRepo.findByBatchCode("261008-BLD-B1")).thenReturn(Optional.of(batch(BatchSource.CLONES)));

        assertThrows(BatchRecordException.class, () -> service.approveRun(10L, manager, "ok"));
        verify(runService, never()).authorizeByManager(any(), any(), any());
    }

    @Test
    void approve_newVariety_isCreatedOnApproval() {
        BatchRecordEntry e = entry(BatchAction.CLONING);
        e.setNewVarietyName("Gelato");
        e.setNewVarietyCode("GEL");
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));
        when(varietyRepo.findByNameIgnoreCase("Gelato")).thenReturn(Optional.empty());
        when(varietyRepo.findByCode("GEL")).thenReturn(Optional.empty());
        when(varietyRepo.save(any(Variety.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepo.findByBatchCode("261008-GEL-B1")).thenReturn(Optional.empty());
        when(batchRepo.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));

        service.approveRun(10L, manager, "ok");

        verify(varietyRepo).save(argThat(v -> v.getName().equals("Gelato") && v.getCode().equals("GEL")));
        assertEquals("261008-GEL-B1", e.getBatch().getBatchCode());
    }

    @Test
    void approve_destructionNoLongerCovered_isBlocked() {
        Batch b = batch(BatchSource.CLONES);
        BatchRecordEntry e = entry(BatchAction.DESTRUCTION);
        e.setBatch(b);
        e.setMaterial(DestructionMaterial.FAN_LEAVES);
        e.setWeightKg(1.0);
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));
        when(entryRepo.findByBatchAndStatus(b, BatchEntryStatus.APPROVED)).thenReturn(new ArrayList<>());

        assertThrows(BatchRecordException.class, () -> service.approveRun(10L, manager, "ok"));
        assertEquals(BatchEntryStatus.PENDING, e.getStatus());
        verify(runService, never()).authorizeByManager(any(), any(), any());
    }

    @Test
    void approve_runWithoutBatchEntry_justApprovesRun() {
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.empty());

        service.approveRun(10L, manager, "ok");

        verify(runService).authorizeByManager(10L, manager, "ok");
    }

    @Test
    void reject_voidsEntry() {
        BatchRecordEntry e = entry(BatchAction.HARVEST);
        when(entryRepo.findByRunId(10L)).thenReturn(Optional.of(e));

        service.rejectRun(10L, manager, "redo");

        assertEquals(BatchEntryStatus.REJECTED, e.getStatus());
        verify(runService).rejectByManager(eq(10L), eq(manager), eq("redo"));
    }
}
