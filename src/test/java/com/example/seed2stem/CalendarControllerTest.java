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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private ChecklistRunRepository checklistRunRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CalendarController controller;

    private MockHttpSession session;
    private User managerUser;
    private User tech;
    private User otherTech;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(1L);
        tech = new User("tech", "h", "Tech", "User", AccountType.TECHNICIAN);
        tech.setId(2L);
        otherTech = new User("tech2", "h", "Other", "Tech", AccountType.TECHNICIAN);
        otherTech.setId(3L);
    }

    private ChecklistRun run(long id, User who, ChecklistRunStatus status) {
        Task task = new Task();
        task.setTitle("AM Inspection");
        ChecklistRun run = new ChecklistRun();
        run.setId(id);
        run.setTask(task);
        run.setCompletedBy(who);
        run.setChecklistName("AM Inspection");
        run.setStatus(status);
        run.setStartTime(LocalDateTime.of(2026, 3, 15, 9, 0));
        run.setEndTime(LocalDateTime.of(2026, 3, 15, 10, 0));
        return run;
    }

    // --- calendarPage ---

    @Test
    void calendarPage_noUser_redirectsToLogin() {
        assertEquals("redirect:/auth/login", controller.calendarPage(session, new ConcurrentModel()));
    }

    @Test
    void calendarPage_manager_getsPersonFilter() {
        session.setAttribute("loggedInUser", managerUser);
        when(userRepository.findByAccountType(AccountType.TECHNICIAN)).thenReturn(List.of(tech, otherTech));
        when(userRepository.findByAccountType(AccountType.MANAGER)).thenReturn(List.of(managerUser));
        Model model = new ConcurrentModel();

        assertEquals("calendar", controller.calendarPage(session, model));
        assertEquals(true, model.getAttribute("isStaff"));
        assertEquals(3, ((List<?>) model.getAttribute("people")).size());
    }

    @Test
    void calendarPage_technician_hasNoPersonFilter() {
        session.setAttribute("loggedInUser", tech);
        Model model = new ConcurrentModel();

        assertEquals("calendar", controller.calendarPage(session, model));
        assertEquals(false, model.getAttribute("isStaff"));
        assertNull(model.getAttribute("people"));
    }

    // --- getEvents ---

    @Test
    void getEvents_noUser_returns401() {
        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", null, session);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void getEvents_manager_getsEveryonesRunsColouredByStatus() {
        session.setAttribute("loggedInUser", managerUser);
        when(checklistRunRepository.findByStartTimeBetween(any(), any()))
                .thenReturn(List.of(run(10, tech, ChecklistRunStatus.APPROVED), run(11, otherTech, ChecklistRunStatus.PENDING)));

        List<CalendarEventDTO> events = controller.getEvents("2026-03-01", "2026-03-31", null, session).getBody();

        assertEquals(2, events.size());
        CalendarEventDTO approved = events.get(0);
        assertEquals("AM Inspection", approved.getTitle());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("Approved", approved.getStatusLabel());
        assertEquals("#2e7d32", approved.getColor());
        assertEquals("Tech User", approved.getPerson());
        assertEquals("/checklist-runs/runs/10", approved.getUrl());
        assertEquals("#d98a00", events.get(1).getColor());
    }

    @Test
    void getEvents_manager_canFilterToOnePerson() {
        session.setAttribute("loggedInUser", managerUser);
        when(checklistRunRepository.findByStartTimeBetween(any(), any()))
                .thenReturn(List.of(run(10, tech, ChecklistRunStatus.APPROVED), run(11, otherTech, ChecklistRunStatus.PENDING)));

        List<CalendarEventDTO> events = controller.getEvents("2026-03-01", "2026-03-31", 3L, session).getBody();

        assertEquals(1, events.size());
        assertEquals("Other Tech", events.get(0).getPerson());
    }

    @Test
    void getEvents_technician_onlyGetsOwnRuns() {
        session.setAttribute("loggedInUser", tech);
        when(checklistRunRepository.findByUserAndStartTimeBetween(eq(tech), any(), any()))
                .thenReturn(List.of(run(10, tech, ChecklistRunStatus.IN_PROGRESS)));

        List<CalendarEventDTO> events = controller.getEvents("2026-03-01", "2026-03-31", null, session).getBody();

        assertEquals(1, events.size());
        assertEquals("In progress", events.get(0).getStatusLabel());
        verify(checklistRunRepository, never()).findByStartTimeBetween(any(), any());
    }
}
