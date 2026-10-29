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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeClockRulesTest {

    @Mock
    private TimeEntryRepository timeEntryRepo;

    @Mock
    private ChecklistRunRepository runRepo;

    @InjectMocks
    private TimeEntryService service;

    private User tech;
    private TimeEntry shift;

    @BeforeEach
    void setUp() {
        tech = new User("tech", "h", "Tech", "User", AccountType.TECHNICIAN);
        tech.setId(1L);
        shift = new TimeEntry();
        shift.setUser(tech);
        shift.setDate(LocalDate.now());
        shift.setClockInTime(LocalDateTime.now().minusHours(2));
    }

    @Test
    void clockOut_withOpenTask_isBlocked() {
        ChecklistRun open = new ChecklistRun();
        open.setChecklistName("AM Inspection");
        when(timeEntryRepo.findByUserAndClockOutTimeIsNull(tech)).thenReturn(Optional.of(shift));
        when(runRepo.findByCompletedByAndStatus(tech, ChecklistRunStatus.IN_PROGRESS)).thenReturn(List.of(open));

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.clockOut(tech, null));

        assertTrue(e.getMessage().contains("AM Inspection"));
        assertNull(shift.getClockOutTime());
        verify(timeEntryRepo, never()).save(any());
    }

    @Test
    void clockOut_withNoOpenTasks_endsShift() {
        when(timeEntryRepo.findByUserAndClockOutTimeIsNull(tech)).thenReturn(Optional.of(shift));
        when(runRepo.findByCompletedByAndStatus(tech, ChecklistRunStatus.IN_PROGRESS)).thenReturn(List.of());
        when(timeEntryRepo.save(shift)).thenReturn(shift);

        service.clockOut(tech, "done");

        assertNotNull(shift.getClockOutTime());
        assertEquals(2.0, shift.getTotalHours(), 0.05);
    }

    @Test
    void clockInRedirect_onlyAllowsLocalPaths() {
        assertTrue(TimeClockController.isSafeRedirect("/tasks/5"));
        assertFalse(TimeClockController.isSafeRedirect("//evil.example"));
        assertFalse(TimeClockController.isSafeRedirect("https://evil.example"));
        assertFalse(TimeClockController.isSafeRedirect("/\\evil.example"));
        assertFalse(TimeClockController.isSafeRedirect(null));
    }
}
