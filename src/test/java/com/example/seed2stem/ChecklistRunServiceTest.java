package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistRunServiceTest {

    @Mock
    private ChecklistRunRepository runRepo;

    @InjectMocks
    private ChecklistRunService runService;

    private User technician;
    private User manager;
    private ChecklistRun pendingRun;
    private ChecklistRun inProgressRun;

    @BeforeEach
    void setUp() {
        technician = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        technician.setId(1L);
        manager = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        manager.setId(2L);

        inProgressRun = new ChecklistRun();
        inProgressRun.setStatus(ChecklistRunStatus.IN_PROGRESS);
        inProgressRun.setCompletedBy(technician);
        inProgressRun.setStartTime(LocalDateTime.now());

        pendingRun = new ChecklistRun();
        pendingRun.setStatus(ChecklistRunStatus.PENDING);
        pendingRun.setCompletedBy(technician);
    }

    // --- getChecklistById ---

    @Test
    void getChecklistById_existingId_returnsRun() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(pendingRun));

        ChecklistRun result = runService.getChecklistById(1L);
        assertNotNull(result);
        assertEquals(ChecklistRunStatus.PENDING, result.getStatus());
    }

    @Test
    void getChecklistById_nonExistingId_throwsException() {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> runService.getChecklistById(999L));
    }

    // --- submitByTechnician ---

    @Test
    void submitByTechnician_inProgressRun_setsStatusToPending() {
        ChecklistItem item = new ChecklistItem();
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(item);
        resp.setBooleanAnswer(true);

        inProgressRun.setResponses(List.of(resp));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistRun result = runService.submitByTechnician(inProgressRun);

        assertEquals(ChecklistRunStatus.PENDING, result.getStatus());
        assertNotNull(result.getEndTime());
    }

    @Test
    void submitByTechnician_rejectedRun_canBeResubmitted() {
        ChecklistRun rejectedRun = new ChecklistRun();
        rejectedRun.setStatus(ChecklistRunStatus.REJECTED);

        ChecklistItem item = new ChecklistItem();
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.TEXT);

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(item);
        resp.setTextAnswer("fixed answer");

        rejectedRun.setResponses(List.of(resp));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistRun result = runService.submitByTechnician(rejectedRun);
        assertEquals(ChecklistRunStatus.PENDING, result.getStatus());
    }

    @Test
    void submitByTechnician_approvedRun_throwsException() {
        ChecklistRun approvedRun = new ChecklistRun();
        approvedRun.setStatus(ChecklistRunStatus.APPROVED);
        approvedRun.setResponses(List.of());

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(approvedRun));
    }

    @Test
    void submitByTechnician_pendingRun_throwsException() {
        pendingRun.setResponses(List.of());

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(pendingRun));
    }

    @Test
    void submitByTechnician_emptyResponses_throwsException() {
        inProgressRun.setResponses(new ArrayList<>());

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(inProgressRun));
    }

    @Test
    void submitByTechnician_nullResponses_throwsException() {
        inProgressRun.setResponses(null);

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(inProgressRun));
    }

    @Test
    void submitByTechnician_missingBooleanAnswer_throwsException() {
        ChecklistItem item = new ChecklistItem();
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(item);
        resp.setBooleanAnswer(null); // missing

        inProgressRun.setResponses(List.of(resp));

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(inProgressRun));
    }

    @Test
    void submitByTechnician_missingNumericAnswer_throwsException() {
        ChecklistItem item = new ChecklistItem();
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.INTEGER);

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(item);
        resp.setNumericAnswer(null); // missing

        inProgressRun.setResponses(List.of(resp));

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(inProgressRun));
    }

    @Test
    void submitByTechnician_responseWithNullItem_throwsException() {
        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(null);

        inProgressRun.setResponses(List.of(resp));

        assertThrows(RuntimeException.class, () -> runService.submitByTechnician(inProgressRun));
    }

    // --- pauseByUser ---

    @Test
    void pauseByUser_inProgressRun_savesResponses() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(inProgressRun));
        inProgressRun.setResponses(new ArrayList<>());
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ChecklistResponse> responses = new ArrayList<>();
        ChecklistResponse resp = new ChecklistResponse();
        resp.setTextAnswer("partial");
        responses.add(resp);

        ChecklistRun result = runService.pauseByUser(1L, responses);

        assertNotNull(result);
        assertEquals(1, result.getResponses().size());
        verify(runRepo).save(any());
    }

    @Test
    void pauseByUser_pendingRun_throwsException() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(pendingRun));

        assertThrows(RuntimeException.class,
                () -> runService.pauseByUser(1L, new ArrayList<>()));
    }

    @Test
    void pauseByUser_nonExistingRun_throwsException() {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> runService.pauseByUser(999L, new ArrayList<>()));
    }

    // --- authorizeByManager ---

    @Test
    void authorizeByManager_pendingRun_setsApproved() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(pendingRun));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistRun result = runService.authorizeByManager(1L, manager, "Looks good");

        assertEquals(ChecklistRunStatus.APPROVED, result.getStatus());
        assertEquals(manager, result.getAuthorizedBy());
        assertNotNull(result.getAuthorizedAt());
        assertEquals("Looks good", result.getManagerComments());
    }

    @Test
    void authorizeByManager_nonPendingRun_throwsException() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(inProgressRun));

        assertThrows(RuntimeException.class,
                () -> runService.authorizeByManager(1L, manager, "comment"));
    }

    // --- rejectByManager ---

    @Test
    void rejectByManager_pendingRun_setsRejected() {
        when(runRepo.findById(1L)).thenReturn(Optional.of(pendingRun));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistRun result = runService.rejectByManager(1L, manager, "Redo section 3");

        assertEquals(ChecklistRunStatus.REJECTED, result.getStatus());
        assertEquals(manager, result.getAuthorizedBy());
        assertEquals("Redo section 3", result.getManagerComments());
    }

    @Test
    void rejectByManager_approvedRun_throwsException() {
        ChecklistRun approvedRun = new ChecklistRun();
        approvedRun.setStatus(ChecklistRunStatus.APPROVED);
        when(runRepo.findById(1L)).thenReturn(Optional.of(approvedRun));

        assertThrows(RuntimeException.class,
                () -> runService.rejectByManager(1L, manager, "comment"));
    }

    // --- getPendingChecklists ---

    @Test
    void getPendingChecklists_returnsPendingRuns() {
        when(runRepo.findByStatus(ChecklistRunStatus.PENDING))
                .thenReturn(List.of(pendingRun));

        List<ChecklistRun> result = runService.getPendingChecklists();

        assertEquals(1, result.size());
        assertEquals(ChecklistRunStatus.PENDING, result.get(0).getStatus());
    }

    // --- getActiveRunsForUser ---

    @Test
    void getActiveRunsForUser_returnsInProgressRuns() {
        when(runRepo.findByCompletedByAndStatus(technician, ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of(inProgressRun));

        List<ChecklistRun> result = runService.getActiveRunsForUser(technician);

        assertEquals(1, result.size());
        assertEquals(ChecklistRunStatus.IN_PROGRESS, result.get(0).getStatus());
    }

    // --- findExistingInProgressRun ---

    @Test
    void findExistingInProgressRun_found_returnsOptional() {
        Task task = new Task();
        when(runRepo.findByCompletedByAndTaskAndStatus(technician, task, ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgressRun));

        Optional<ChecklistRun> result = runService.findExistingInProgressRun(technician, task);

        assertTrue(result.isPresent());
    }

    @Test
    void findExistingInProgressRun_notFound_returnsEmpty() {
        Task task = new Task();
        when(runRepo.findByCompletedByAndTaskAndStatus(technician, task, ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        Optional<ChecklistRun> result = runService.findExistingInProgressRun(technician, task);

        assertTrue(result.isEmpty());
    }
}
