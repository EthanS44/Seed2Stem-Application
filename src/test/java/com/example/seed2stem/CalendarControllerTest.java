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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private ChecklistRunRepository checklistRunRepository;

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
    void getEvents_loggedIn_returnsChecklistRunEvents() {
        session.setAttribute("loggedInUser", managerUser);

        User tech = new User("tech", "h", "Tech", "User", AccountType.TECHNICIAN);
        ChecklistRun run = new ChecklistRun();
        run.setCompletedBy(tech);
        run.setChecklistName("AM Inspection");
        run.setStartTime(LocalDateTime.of(2026, 3, 15, 9, 0));
        run.setEndTime(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(checklistRunRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of(run));

        ResponseEntity<List<CalendarEventDTO>> result =
                controller.getEvents("2026-03-01", "2026-03-31", session);

        List<CalendarEventDTO> events = result.getBody();
        assertEquals(1, events.size());
        assertEquals("checklist-run", events.get(0).getEventType());
        assertEquals("#0277bd", events.get(0).getColor());
        assertTrue(events.get(0).getTitle().contains("AM Inspection"));
        assertTrue(events.get(0).getTitle().contains("Tech User"));
    }

}
