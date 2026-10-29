package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    static final LocalDate ANCHOR = LocalDate.of(2026, 1, 5); // a Monday

    @Mock
    private TimeEntryRepository timeEntryRepo;

    @Mock
    private UserRepository userRepo;

    private PayrollService service;
    private User tech;
    private final List<TimeEntry> entries = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new PayrollService(timeEntryRepo, userRepo, ANCHOR.toString(), 40);
        tech = new User("tech", "h", "Tess", "Tech", AccountType.TECHNICIAN);
        tech.setId(1L);
        lenient().when(userRepo.findByAccountType(AccountType.TECHNICIAN)).thenReturn(List.of(tech));
        lenient().when(userRepo.findByAccountType(AccountType.MANAGER)).thenReturn(List.of());
        lenient().when(timeEntryRepo.findAllByDateRange(any(), any())).thenAnswer(inv -> {
            LocalDate from = inv.getArgument(0), to = inv.getArgument(1);
            return entries.stream().filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to)).toList();
        });
    }

    private TimeEntry shift(LocalDate day, int startHour, int hours) {
        TimeEntry e = new TimeEntry();
        e.setUser(tech);
        e.setDate(day);
        e.setClockInTime(day.atTime(startHour, 0));
        e.setClockOutTime(day.atTime(startHour, 0).plusHours(hours));
        entries.add(e);
        return e;
    }

    // --- periods ---

    @Test
    void week_runsMondayToSunday() {
        PayPeriod p = PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 29), ANCHOR);
        assertEquals(LocalDate.of(2026, 10, 26), p.getStart());
        assertEquals(LocalDate.of(2026, 11, 1), p.getEnd());
        assertEquals("Oct 26 – Nov 1, 2026", p.getLabel());
    }

    @Test
    void biweekly_stepsFromAnchor_evenBeforeIt() {
        PayPeriod p = PayPeriod.containing(PayPeriod.Type.BIWEEKLY, LocalDate.of(2026, 10, 29), ANCHOR);
        assertEquals(LocalDate.of(2026, 10, 26), p.getStart());
        assertEquals(LocalDate.of(2026, 11, 8), p.getEnd());
        assertEquals(LocalDate.of(2026, 10, 12), p.previous(ANCHOR).getStart());

        PayPeriod before = PayPeriod.containing(PayPeriod.Type.BIWEEKLY, LocalDate.of(2026, 1, 1), ANCHOR);
        assertEquals(LocalDate.of(2025, 12, 22), before.getStart());
    }

    @Test
    void semimonthly_splitsOnThe15th() {
        PayPeriod first = PayPeriod.containing(PayPeriod.Type.SEMIMONTHLY, LocalDate.of(2026, 2, 3), ANCHOR);
        assertEquals(LocalDate.of(2026, 2, 15), first.getEnd());
        PayPeriod second = first.next(ANCHOR);
        assertEquals(LocalDate.of(2026, 2, 16), second.getStart());
        assertEquals(LocalDate.of(2026, 2, 28), second.getEnd());
        assertEquals(LocalDate.of(2026, 3, 1), second.next(ANCHOR).getStart());
    }

    @Test
    void custom_swapsReversedDates_andCapsLength() {
        PayPeriod p = PayPeriod.custom(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1));
        assertEquals(LocalDate.of(2026, 3, 1), p.getStart());
        assertEquals(10, p.getDays());
        assertEquals(LocalDate.of(2026, 2, 19), p.previous(ANCHOR).getStart());

        assertEquals(PayPeriod.MAX_CUSTOM_DAYS, PayPeriod.custom(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)).getDays());
    }

    @Test
    void buckets_dailyForShortPeriods_weeklyForMonths() {
        assertEquals(7, PayrollService.buckets(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 29), ANCHOR)).size());
        List<PayrollReport.Bucket> oct = PayrollService.buckets(PayPeriod.containing(PayPeriod.Type.MONTH, LocalDate.of(2026, 10, 1), ANCHOR));
        // Oct 2026 starts on a Thursday: 1–4, 5–11, 12–18, 19–25, 26–31
        assertEquals(5, oct.size());
        assertEquals(LocalDate.of(2026, 10, 4), oct.get(0).getEnd());
        assertEquals(LocalDate.of(2026, 10, 31), oct.get(4).getEnd());
    }

    // --- hours and overtime ---

    @Test
    void overtime_isHoursOver40InTheWeek() {
        LocalDate mon = LocalDate.of(2026, 10, 19);
        for (int i = 0; i < 5; i++) shift(mon.plusDays(i), 8, 9); // 45 h

        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, mon, ANCHOR), LocalDateTime.of(2026, 10, 29, 12, 0));
        PayrollReport.EmployeeHours e = r.getEmployees().get(0);

        assertEquals(45 * 60, e.getTotalMinutes());
        assertEquals(40 * 60, e.getRegularMinutes());
        assertEquals(5 * 60, e.getOvertimeMinutes());
        // Overtime lands on the last shift of the week
        assertEquals(5 * 60, e.getShifts().get(4).getOvertimeMinutes());
        assertEquals(9 * 60, e.bucket(0));
    }

    @Test
    void overtime_countsHoursWorkedEarlierInTheWeek_outsideThePeriod() {
        // Mon Sep 28 – Wed Sep 30: 36 h. Thu Oct 1: 8 h, of which 4 h is overtime.
        LocalDate mon = LocalDate.of(2026, 9, 28);
        shift(mon, 6, 12);
        shift(mon.plusDays(1), 6, 12);
        shift(mon.plusDays(2), 6, 12);
        shift(LocalDate.of(2026, 10, 1), 8, 8);

        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.MONTH, LocalDate.of(2026, 10, 1), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 12, 0));
        PayrollReport.EmployeeHours e = r.getEmployees().get(0);

        assertEquals(1, e.getShifts().size());
        assertEquals(8 * 60, e.getTotalMinutes());
        assertEquals(4 * 60, e.getOvertimeMinutes());
    }

    @Test
    void openShiftFromEarlierDay_countsNothing_andIsFlagged() {
        TimeEntry e = shift(LocalDate.of(2026, 10, 27), 8, 0);
        e.setClockOutTime(null);

        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 27), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 12, 0));

        PayrollReport.Shift s = r.getEmployees().get(0).getShifts().get(0);
        assertEquals(0, s.getMinutes());
        assertTrue(s.getFlags().contains(PayrollReport.Shift.Flag.MISSING_CLOCK_OUT));
        assertEquals(1, r.getFlaggedCount());
    }

    @Test
    void openShiftToday_countsToNow_andIsNotAProblem() {
        TimeEntry e = shift(LocalDate.of(2026, 10, 29), 8, 0);
        e.setClockOutTime(null);

        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 29), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 11, 30));

        PayrollReport.EmployeeHours eh = r.getEmployees().get(0);
        assertEquals(210, eh.getTotalMinutes());
        assertTrue(eh.isClockedIn());
        assertEquals(0, r.getFlaggedCount());
        assertEquals(1, r.getClockedInCount());
    }

    @Test
    void autoClockedOutAndLongShifts_areFlagged() {
        TimeEntry e = shift(LocalDate.of(2026, 10, 26), 6, 14);
        e.setNotes("Auto clocked out");

        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 26), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 12, 0));

        List<PayrollReport.Shift.Flag> flags = r.getEmployees().get(0).getShifts().get(0).getFlags();
        assertTrue(flags.contains(PayrollReport.Shift.Flag.AUTO_CLOCKED_OUT));
        assertTrue(flags.contains(PayrollReport.Shift.Flag.LONG_SHIFT));
    }

    @Test
    void employeesWithNoHours_stillListed() {
        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 26), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 12, 0));
        assertEquals(1, r.getEmployees().size());
        assertEquals(0, r.getEmployeesWorked());
        assertEquals("0.00", PayrollReport.hours(r.getTotalMinutes()));
    }

    @Test
    void export_buildsAWorkbook() {
        shift(LocalDate.of(2026, 10, 26), 8, 8);
        PayrollReport r = service.build(PayPeriod.containing(PayPeriod.Type.WEEK, LocalDate.of(2026, 10, 26), ANCHOR),
                LocalDateTime.of(2026, 10, 29, 12, 0));
        byte[] bytes = new PayrollExportService().export(r);
        assertTrue(bytes.length > 0);
    }

    // --- controller access ---

    @Test
    void controller_onlyManagersAndDevelopers() {
        PayrollController controller = new PayrollController(service, new PayrollExportService(),
                mock(TimeEntryService.class), timeEntryRepo);
        MockHttpSession session = new MockHttpSession();
        assertEquals("redirect:/auth/login", controller.dashboard(null, null, null, null, session, new ConcurrentModel()));

        session.setAttribute("loggedInUser", tech);
        assertEquals("redirect:/dashboard/home-dashboard", controller.dashboard(null, null, null, null, session, new ConcurrentModel()));
        assertEquals(403, controller.export(null, null, null, null, session).getStatusCode().value());

        User mgr = new User("m", "h", "Max", "Manager", AccountType.MANAGER);
        mgr.setId(2L);
        session.setAttribute("loggedInUser", mgr);
        assertEquals("payroll", controller.dashboard("week", LocalDate.of(2026, 10, 26), null, null, session, new ConcurrentModel()));
    }

    @Test
    void controller_managerCantEditOwnHours() {
        TimeEntryService tes = mock(TimeEntryService.class);
        PayrollController controller = new PayrollController(service, new PayrollExportService(), tes, timeEntryRepo);
        User mgr = new User("m", "h", "Max", "Manager", AccountType.MANAGER);
        mgr.setId(2L);
        TimeEntry own = new TimeEntry();
        own.setId(9L);
        own.setUser(mgr);
        when(timeEntryRepo.findById(9L)).thenReturn(java.util.Optional.of(own));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", mgr);

        String result = controller.editEntry(9L, "2026-10-26T08:00", "2026-10-26T20:00", null, "period=week&date=2026-10-26",
                session, new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap());

        assertEquals("redirect:/payroll/employees/2?period=week&date=2026-10-26", result);
        verify(tes, never()).updateEntry(any(), any(), any(), any());
    }
}
