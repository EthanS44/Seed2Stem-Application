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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private ChecklistRunService checklistRunService;

    @Mock
    private BatchService batchService;

    @Mock
    private UserRepository userRepo;

    @Mock
    private TimeEntryService timeEntryService;

    @Mock
    private TaskPauseRepository taskPauseRepository;

    @InjectMocks
    private DashboardController controller;

    private MockHttpSession session;
    private User managerUser;
    private User techUser;
    private User developerUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(1L);
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(2L);
        developerUser = new User("dev", "hashed", "Dev", "User", AccountType.DEVELOPER);
        developerUser.setId(3L);
    }

    // --- homeDashboard ---

    @Test
    void homeDashboard_noUser_redirectsToLogin() {
        String result = controller.homeDashboard(session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void homeDashboard_manager_redirectsToManagerDashboard() {
        session.setAttribute("loggedInUser", managerUser);

        String result = controller.homeDashboard(session);
        assertEquals("redirect:/dashboard/manager-dashboard", result);
    }

    @Test
    void homeDashboard_technician_redirectsToTechnicianDashboard() {
        session.setAttribute("loggedInUser", techUser);

        String result = controller.homeDashboard(session);
        assertEquals("redirect:/dashboard/technician-dashboard", result);
    }

    @Test
    void homeDashboard_developer_redirectsToDeveloperDashboard() {
        session.setAttribute("loggedInUser", developerUser);

        String result = controller.homeDashboard(session);
        assertEquals("redirect:/dashboard/developer-dashboard", result);
    }

    // --- technicianDashboard ---

    @Test
    void technicianDashboard_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();

        String result = controller.technicianDashboard(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void technicianDashboard_loggedIn_populatesModel() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        ChecklistRun run1 = new ChecklistRun();
        ChecklistRun run2 = new ChecklistRun();
        when(checklistRunService.getActiveRunsForUser(techUser))
                .thenReturn(List.of(run1, run2));
        when(timeEntryService.isClockedIn(techUser)).thenReturn(true);

        String result = controller.technicianDashboard(session, model);

        assertEquals("technician-dashboard", result);
        assertEquals(2, model.getAttribute("activeTaskCount"));
        assertEquals(true, model.getAttribute("clockedIn"));
    }

    // --- managerDashboard ---

    @Test
    void managerDashboard_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();

        String result = controller.managerDashboard(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void managerDashboard_loggedIn_populatesModel() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(checklistRunService.getPendingChecklists())
                .thenReturn(List.of(new ChecklistRun(), new ChecklistRun()));
        when(checklistRunService.getAllActiveRuns())
                .thenReturn(List.of(new ChecklistRun(), new ChecklistRun(), new ChecklistRun()));
        when(timeEntryService.countClockedIn()).thenReturn(4L);

        String result = controller.managerDashboard(session, model);

        assertEquals("manager-dashboard", result);
        assertEquals(2, model.getAttribute("pendingCount"));
        assertEquals(3, model.getAttribute("allActiveTaskCount"));
        assertEquals(4L, model.getAttribute("clockedInCount"));
    }

    // --- developerDashboard ---

    @Test
    void developerDashboard_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();

        String result = controller.developerDashboard(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void developerDashboard_loggedIn_populatesModel() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of());
        when(checklistRunService.getAllActiveRuns()).thenReturn(List.of(new ChecklistRun()));
        when(timeEntryService.countClockedIn()).thenReturn(1L);

        String result = controller.developerDashboard(session, model);

        assertEquals("developer-dashboard", result);
        assertEquals(0, model.getAttribute("pendingCount"));
        assertEquals(1, model.getAttribute("allActiveTaskCount"));
        assertEquals(1L, model.getAttribute("clockedInCount"));
    }

    // --- taskDashboard ---

    @Test
    void taskDashboard_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();

        String result = controller.taskDashboard(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void taskDashboard_technician_doesNotAddPendingRuns() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        when(taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc()).thenReturn(List.of());
        when(checklistRunService.getActiveRunsForUser(techUser)).thenReturn(List.of());

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        assertEquals(false, model.getAttribute("isManager"));
        assertNull(model.getAttribute("pendingRuns"));
    }

    @Test
    void taskDashboard_manager_addsPendingRuns() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc()).thenReturn(List.of());
        when(checklistRunService.getAllActiveRuns()).thenReturn(List.of());
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of(new ChecklistRun()));

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        assertEquals(true, model.getAttribute("isManager"));
        assertNotNull(model.getAttribute("pendingRuns"));
        assertNotNull(model.getAttribute("pausedRunIds"));
    }

    @Test
    void taskDashboard_developer_addsPendingRuns() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc()).thenReturn(List.of());
        when(checklistRunService.getAllActiveRuns()).thenReturn(List.of());
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of(new ChecklistRun()));

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        assertEquals(true, model.getAttribute("isManager"));
        assertNotNull(model.getAttribute("pendingRuns"));
    }

    @Test
    void taskDashboard_manager_seesAllActiveRunsFromAllUsers() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();

        ChecklistRun techRun = new ChecklistRun();
        techRun.setId(100L);
        techRun.setCompletedBy(techUser);
        ChecklistRun otherRun = new ChecklistRun();
        otherRun.setId(101L);
        otherRun.setCompletedBy(developerUser);

        when(taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc()).thenReturn(List.of());
        when(checklistRunService.getAllActiveRuns()).thenReturn(List.of(techRun, otherRun));
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of(101L));
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of());

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        @SuppressWarnings("unchecked")
        List<ChecklistRun> activeTasks = (List<ChecklistRun>) model.getAttribute("activeTasks");
        assertNotNull(activeTasks);
        assertEquals(2, activeTasks.size());
        @SuppressWarnings("unchecked")
        java.util.Set<Long> pausedRunIds = (java.util.Set<Long>) model.getAttribute("pausedRunIds");
        assertNotNull(pausedRunIds);
        assertTrue(pausedRunIds.contains(101L));
        assertFalse(pausedRunIds.contains(100L));
        verify(checklistRunService).getAllActiveRuns();
        verify(checklistRunService, never()).getActiveRunsForUser(any());
    }
}
