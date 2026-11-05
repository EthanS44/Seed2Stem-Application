package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private ChecklistRunRepository runRepo;
    @Mock private ChecklistRunService runService;
    @Mock private TimeEntryService timeEntryService;
    @Mock private BatchRecordService batchRecordService;
    @Mock private PayrollService payrollService;
    @Mock private RegistrationRequestRepository registrationRequestRepo;
    @Mock private PasswordResetRequestRepository passwordResetRequestRepo;
    @Mock private TicketRepository ticketRepo;

    @InjectMocks
    private NotificationService service;

    private User tech;
    private User manager;

    @BeforeEach
    void setUp() {
        tech = new User("tech", "h", "Sam", "Rivera", AccountType.TECHNICIAN);
        tech.setId(1L);
        manager = new User("mgr", "h", "Pat", "Kim", AccountType.MANAGER);
        manager.setId(2L);
        lenient().when(runRepo.findRejectedByUser(any())).thenReturn(List.of());
        lenient().when(runRepo.findRecentSubmittedByUser(any(), any())).thenReturn(List.of());
        lenient().when(timeEntryService.getActiveEntry(any())).thenReturn(Optional.empty());
    }

    @Test
    void technician_seesRejectionsApprovalsAndAForgottenClockOut() {
        ChecklistRun fixMe = run(task(false), ChecklistRunStatus.REJECTED, LocalDateTime.now().minusDays(30), "Redo the B2 reading");
        ChecklistRun oldOwn = run(task(true), ChecklistRunStatus.REJECTED, LocalDateTime.now().minusDays(30), null);
        ChecklistRun approved = run(task(false), ChecklistRunStatus.APPROVED, LocalDateTime.now().minusHours(2), null);
        ChecklistRun oldApproved = run(task(false), ChecklistRunStatus.APPROVED, LocalDateTime.now().minusDays(3), null);
        when(runRepo.findRejectedByUser(tech)).thenReturn(List.of(fixMe, oldOwn));
        when(runRepo.findRecentSubmittedByUser(eq(tech), any())).thenReturn(List.of(approved, oldApproved));
        TimeEntry open = new TimeEntry();
        open.setDate(LocalDate.now().minusDays(1));
        when(timeEntryService.getActiveEntry(tech)).thenReturn(Optional.of(open));

        List<Notification> alerts = service.forUser(tech);

        assertEquals(3, alerts.size());
        assertEquals(Notification.Level.DANGER, alerts.get(0).getLevel());
        assertEquals("Pat Kim: “Redo the B2 reading”", alerts.get(0).getDetail());
        assertEquals("Fix and resubmit", alerts.get(0).getAction());
        assertEquals(Notification.Level.WARN, alerts.get(1).getLevel());
        assertTrue(alerts.get(1).getTitle().startsWith("You're still clocked in from"));
        assertEquals(Notification.Level.OK, alerts.get(2).getLevel());
        verifyNoInteractions(payrollService, runService, batchRecordService);
    }

    @Test
    void manager_seesApprovalsPayrollAndFilters() {
        ChecklistRun waiting = new ChecklistRun();
        waiting.setEndTime(LocalDateTime.now().minusDays(2));
        when(runService.getPendingChecklists()).thenReturn(List.of(waiting));
        PayPeriod week = PayPeriod.custom(LocalDate.now(), LocalDate.now());
        when(payrollService.resolvePeriod(eq("week"), any(), any(), any())).thenReturn(week);
        PayrollReport report = mock(PayrollReport.class);
        when(report.getFlaggedCount()).thenReturn(2L);
        when(payrollService.build(eq(week), any())).thenReturn(report);
        when(batchRecordService.filterStatuses()).thenReturn(Map.of(Room.B1,
                new FilterStatus(Room.B1, LocalDate.now().minusDays(20), LocalDate.now())));

        List<Notification> alerts = service.forUser(manager);

        assertEquals(List.of("Room B1 filters are overdue", "1 task is waiting for approval",
                        "2 shifts need checking before payroll"),
                alerts.stream().map(Notification::getTitle).toList());
        verifyNoInteractions(registrationRequestRepo, ticketRepo);
    }

    private static Task task(boolean userCreated) {
        Task t = new Task();
        t.setTitle("AM Inspection");
        t.setUserCreated(userCreated);
        return t;
    }

    private ChecklistRun run(Task task, ChecklistRunStatus status, LocalDateTime decidedAt, String comment) {
        ChecklistRun r = new ChecklistRun();
        r.setId((long) (Math.random() * 100000));
        r.setTask(task);
        r.setStatus(status);
        r.setAuthorizedAt(decidedAt);
        r.setAuthorizedBy(manager);
        r.setManagerComments(comment);
        return r;
    }
}
