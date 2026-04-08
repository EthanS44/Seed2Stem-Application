package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ChecklistRunRepository checklistRunRepository;

    @Mock
    private BatchActivityLogRepository activityLogRepository;

    @Mock
    private WorkerSessionRepository workerSessionRepository;

    @InjectMocks
    private CalendarController controller;

    private MockHttpSession session;
    private User managerUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(1L);
    }

    // --- calendarPage ---

    @Test
    void calendarPage_noUser_redirectsToLogin() {
        assertEquals("redirect:/auth/login", controller.calendarPage(session));
    }

    @Test
    void calendarPage_loggedIn_returnsCalendarView() {
        session.setAttribute("loggedInUser", managerUser);
        assertEquals("calendar", controller.calendarPage(session));
    }

    // --- getEvents ---

    @Test
    void getEvents_noUser_returns401() {
        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void getEvents_loggedIn_returnsBatchStartEvents() {
        session.setAttribute("loggedInUser", managerUser);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchCode("26-074-BLD-B1-316-01");
        batch.setStrain("Blue Dream");
        batch.setStartDate(LocalDate.of(2026, 3, 15));
        batch.setHarvestDate(null);

        when(batchRepository.findByDateRange(any(), any())).thenReturn(List.of(batch));
        when(checklistRunRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of());
        when(activityLogRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());
        when(workerSessionRepository.findBySessionDateBetween(any(), any())).thenReturn(List.of());

        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        List<CalendarEventDTO> events = result.getBody();
        assertEquals(1, events.size());
        assertEquals("batch-start-1", events.get(0).getId());
        assertEquals("#2e7d32", events.get(0).getColor());
        assertEquals("batch-start", events.get(0).getEventType());
    }

    @Test
    void getEvents_loggedIn_returnsBatchHarvestEvents() {
        session.setAttribute("loggedInUser", managerUser);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchCode("26-074-BLD-B1-316-01");
        batch.setStrain("Blue Dream");
        batch.setStartDate(LocalDate.of(2026, 1, 15));
        batch.setHarvestDate(LocalDate.of(2026, 3, 20));

        when(batchRepository.findByDateRange(any(), any())).thenReturn(List.of(batch));
        when(checklistRunRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of());
        when(activityLogRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());
        when(workerSessionRepository.findBySessionDateBetween(any(), any())).thenReturn(List.of());

        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        List<CalendarEventDTO> events = result.getBody();
        // Should have both start and harvest events
        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> e.getEventType().equals("batch-harvest")));
    }

    @Test
    void getEvents_loggedIn_returnsChecklistRunEvents() {
        session.setAttribute("loggedInUser", managerUser);

        User tech = new User("tech", "h", "Tech", "User", AccountType.TECHNICIAN);
        ChecklistRun run = new ChecklistRun();
        run.setCompletedBy(tech);
        run.setChecklistName("AM Inspection");
        run.setStartTime(LocalDateTime.of(2026, 3, 15, 9, 0));
        run.setEndTime(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(batchRepository.findByDateRange(any(), any())).thenReturn(List.of());
        when(checklistRunRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of(run));
        when(activityLogRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());
        when(workerSessionRepository.findBySessionDateBetween(any(), any())).thenReturn(List.of());

        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        List<CalendarEventDTO> events = result.getBody();
        assertEquals(1, events.size());
        assertEquals("checklist-run", events.get(0).getEventType());
        assertEquals("#0277bd", events.get(0).getColor());
        assertTrue(events.get(0).getTitle().contains("AM Inspection"));
        assertTrue(events.get(0).getTitle().contains("Tech User"));
    }

    @Test
    void getEvents_loggedIn_returnsWorkerSessionEvents() {
        session.setAttribute("loggedInUser", managerUser);

        Batch batch = new Batch();
        batch.setId(1L);

        WorkerSession ws = new WorkerSession();
        ws.setId(1L);
        ws.setBatch(batch);
        ws.setSessionType("TRIMMING");
        ws.setWorkerName("Jane");
        ws.setSessionDate(LocalDate.of(2026, 3, 15));

        when(batchRepository.findByDateRange(any(), any())).thenReturn(List.of());
        when(checklistRunRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of());
        when(activityLogRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());
        when(workerSessionRepository.findBySessionDateBetween(any(), any())).thenReturn(List.of(ws));

        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        List<CalendarEventDTO> events = result.getBody();
        assertEquals(1, events.size());
        assertEquals("worker-session", events.get(0).getEventType());
        assertEquals("#00695c", events.get(0).getColor());
        assertTrue(events.get(0).getTitle().contains("TRIMMING"));
        assertTrue(events.get(0).getTitle().contains("Jane"));
    }
}
