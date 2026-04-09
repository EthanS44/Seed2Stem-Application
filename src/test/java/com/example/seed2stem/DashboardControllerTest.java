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
        when(taskRepo.countByUserCreatedFalse()).thenReturn(5L);
        when(batchService.countActive()).thenReturn(3L);

        String result = controller.technicianDashboard(session, model);

        assertEquals("technician-dashboard", result);
        assertEquals(5L, model.getAttribute("taskCount"));
        assertEquals(3L, model.getAttribute("batchCount"));
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
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of(new ChecklistRun(), new ChecklistRun()));
        when(batchService.countActive()).thenReturn(10L);
        when(taskRepo.countByUserCreatedFalse()).thenReturn(7L);
        when(userRepo.countByAccountType(AccountType.TECHNICIAN)).thenReturn(4L);

        String result = controller.managerDashboard(session, model);

        assertEquals("manager-dashboard", result);
        assertEquals(2, model.getAttribute("pendingCount"));
        assertEquals(10L, model.getAttribute("batchCount"));
        assertEquals(7L, model.getAttribute("taskCount"));
        assertEquals(4L, model.getAttribute("technicianCount"));
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
        when(batchService.countActive()).thenReturn(2L);
        when(taskRepo.countByUserCreatedFalse()).thenReturn(3L);
        when(userRepo.countByAccountType(AccountType.TECHNICIAN)).thenReturn(1L);

        String result = controller.developerDashboard(session, model);

        assertEquals("developer-dashboard", result);
        assertEquals(0, model.getAttribute("pendingCount"));
        assertEquals(2L, model.getAttribute("batchCount"));
        assertEquals(3L, model.getAttribute("taskCount"));
        assertEquals(1L, model.getAttribute("technicianCount"));
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
        when(taskRepo.findByUserCreatedFalse()).thenReturn(List.of());
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
        when(taskRepo.findByUserCreatedFalse()).thenReturn(List.of());
        when(checklistRunService.getActiveRunsForUser(managerUser)).thenReturn(List.of());
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of(new ChecklistRun()));

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        assertEquals(true, model.getAttribute("isManager"));
        assertNotNull(model.getAttribute("pendingRuns"));
    }

    @Test
    void taskDashboard_developer_addsPendingRuns() {
        session.setAttribute("loggedInUser", developerUser);
        Model model = new ConcurrentModel();
        when(taskRepo.findByUserCreatedFalse()).thenReturn(List.of());
        when(checklistRunService.getActiveRunsForUser(developerUser)).thenReturn(List.of());
        when(checklistRunService.getPendingChecklists()).thenReturn(List.of(new ChecklistRun()));

        String result = controller.taskDashboard(session, model);

        assertEquals("task-dashboard", result);
        assertEquals(true, model.getAttribute("isManager"));
        assertNotNull(model.getAttribute("pendingRuns"));
    }
}
