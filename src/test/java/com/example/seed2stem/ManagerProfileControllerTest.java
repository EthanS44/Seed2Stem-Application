package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChecklistRunRepository checklistRunRepository;

    @Mock
    private TimeEntryService timeEntryService;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private TaskPauseRepository taskPauseRepository;

    @InjectMocks
    private ManagerProfileController controller;

    private MockHttpSession session;
    private User developerUser;
    private User managerUser;
    private User techUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        developerUser = new User("dev", "hashed", "Dev", "User", AccountType.DEVELOPER);
        developerUser.setId(1L);
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(2L);
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(3L);
    }

    // --- listManagers ---

    @Test
    void listManagers_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.listManagers(session, model));
    }

    @Test
    void listManagers_manager_redirectsToHome() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard", controller.listManagers(session, model));
    }

    @Test
    void listManagers_technician_redirectsToHome() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard", controller.listManagers(session, model));
    }

    @Test
    void listManagers_developer_returnsManagerList() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.MANAGER)).thenReturn(List.of(managerUser));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of());
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of());
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());

        String result = controller.listManagers(session, model);

        assertEquals("manager-list", result);
        @SuppressWarnings("unchecked")
        List<ManagerProfileController.ManagerSummary> summaries =
                (List<ManagerProfileController.ManagerSummary>) model.getAttribute("summaries");
        assertEquals(1, summaries.size());
        ManagerProfileController.ManagerSummary s = summaries.get(0);
        assertEquals("mgr", s.getManager().getEmail());
        assertFalse(s.isClockedIn());
        assertFalse(s.isPaused());
        assertNull(s.getCurrentTaskLabel());
        assertEquals(0L, model.getAttribute("clockedInCount"));
        assertEquals(0L, model.getAttribute("workingCount"));
        assertEquals(0L, model.getAttribute("pausedCount"));
    }

    @Test
    void listManagers_developer_populatesClockedInAndActiveTask() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.MANAGER)).thenReturn(List.of(managerUser));

        TimeEntry activeEntry = new TimeEntry();
        activeEntry.setUser(managerUser);
        activeEntry.setClockInTime(java.time.LocalDateTime.of(2026, 7, 15, 8, 15));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of(activeEntry));

        Task task = new Task();
        task.setTitle("Approve batches");
        ChecklistRun run = new ChecklistRun();
        run.setId(42L);
        run.setCompletedBy(managerUser);
        run.setTask(task);
        run.setStartTime(java.time.LocalDateTime.of(2026, 7, 15, 9, 0));
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of(run));
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());

        controller.listManagers(session, model);

        @SuppressWarnings("unchecked")
        List<ManagerProfileController.ManagerSummary> summaries =
                (List<ManagerProfileController.ManagerSummary>) model.getAttribute("summaries");
        ManagerProfileController.ManagerSummary s = summaries.get(0);
        assertTrue(s.isClockedIn());
        assertFalse(s.isPaused());
        assertEquals("Approve batches", s.getCurrentTaskLabel());
        assertEquals(42L, s.getCurrentRunId());
        assertEquals(1L, model.getAttribute("clockedInCount"));
        assertEquals(1L, model.getAttribute("workingCount"));
        assertEquals(0L, model.getAttribute("pausedCount"));
    }

    @Test
    void listManagers_developer_flagsPausedRun() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.MANAGER)).thenReturn(List.of(managerUser));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of());

        Task task = new Task();
        task.setTitle("Review harvest");
        ChecklistRun run = new ChecklistRun();
        run.setId(7L);
        run.setCompletedBy(managerUser);
        run.setTask(task);
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of(run));
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of(7L));

        controller.listManagers(session, model);

        @SuppressWarnings("unchecked")
        List<ManagerProfileController.ManagerSummary> summaries =
                (List<ManagerProfileController.ManagerSummary>) model.getAttribute("summaries");
        assertTrue(summaries.get(0).isPaused());
        assertEquals(0L, model.getAttribute("workingCount"));
        assertEquals(1L, model.getAttribute("pausedCount"));
    }

    // --- managerProfile ---

    @Test
    void managerProfile_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.managerProfile(2L, session, model));
    }

    @Test
    void managerProfile_manager_redirectsToHome() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard",
                controller.managerProfile(2L, session, model));
    }

    @Test
    void managerProfile_developer_missingManager_redirectsToList() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        assertEquals("redirect:/managers", controller.managerProfile(999L, session, model));
    }

    @Test
    void managerProfile_developer_populatesModel() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(managerUser));
        when(checklistRunRepository.findCompletedByUserOrderByStartTimeDesc(managerUser))
                .thenReturn(List.of());
        when(timeEntryService.getEntriesForDateRange(eq(managerUser), any(), any()))
                .thenReturn(List.of());
        when(timeEntryService.isClockedIn(managerUser)).thenReturn(true);

        String result = controller.managerProfile(2L, session, model);

        assertEquals("manager-profile", result);
        assertEquals(managerUser, model.getAttribute("manager"));
        assertEquals(true, model.getAttribute("mgrClockedIn"));
    }

    // --- timeEntryDetail ---

    @Test
    void timeEntryDetail_developer_setsBackUrlToManagers() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(managerUser));

        TimeEntry entry = new TimeEntry();
        entry.setId(5L);
        entry.setClockInTime(java.time.LocalDateTime.of(2026, 7, 15, 9, 0));
        entry.setClockOutTime(java.time.LocalDateTime.of(2026, 7, 15, 17, 0));
        when(timeEntryRepository.findById(5L)).thenReturn(java.util.Optional.of(entry));
        when(checklistRunRepository.findByUserAndStartTimeBetween(eq(managerUser), any(), any()))
                .thenReturn(List.of());

        String result = controller.timeEntryDetail(2L, 5L, session, model);

        assertEquals("time-entry-detail", result);
        assertEquals("/managers/2", model.getAttribute("backUrl"));
        assertEquals(managerUser, model.getAttribute("technician"));
    }

    @Test
    void timeEntryDetail_manager_redirectsToHome() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard",
                controller.timeEntryDetail(2L, 5L, session, model));
    }

    // --- editTimeEntry ---

    @Test
    void editTimeEntry_manager_redirectsToHome() {
        session.setAttribute("loggedInUser", managerUser);
        var ra = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        assertEquals("redirect:/dashboard/home-dashboard",
                controller.editTimeEntry(2L, 5L, "2026-07-15T09:00", null, null, session, ra));
        verify(timeEntryService, never()).updateEntry(anyLong(), any(), any(), any());
    }

    @Test
    void editTimeEntry_developer_savesAndRedirectsToProfile() {
        session.setAttribute("loggedInUser", developerUser);
        var ra = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String result = controller.editTimeEntry(2L, 5L,
                "2026-07-15T09:00", "2026-07-15T17:00", "note", session, ra);

        assertEquals("redirect:/managers/2", result);
        verify(timeEntryService).updateEntry(eq(5L),
                eq(java.time.LocalDateTime.of(2026, 7, 15, 9, 0)),
                eq(java.time.LocalDateTime.of(2026, 7, 15, 17, 0)),
                eq("note"));
    }

}
