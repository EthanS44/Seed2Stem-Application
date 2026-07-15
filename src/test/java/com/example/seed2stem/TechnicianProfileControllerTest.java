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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianProfileControllerTest {

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
    private TechnicianProfileController controller;

    private MockHttpSession session;
    private User managerUser;
    private User techUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(1L);
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(2L);
    }

    // --- listTechnicians ---

    @Test
    void listTechnicians_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.listTechnicians(session, model));
    }

    @Test
    void listTechnicians_technician_redirectsToHome() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard", controller.listTechnicians(session, model));
    }

    @Test
    void listTechnicians_manager_returnsTechnicianList() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.TECHNICIAN)).thenReturn(List.of(techUser));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of());
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of());
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());

        String result = controller.listTechnicians(session, model);

        assertEquals("technician-list", result);
        @SuppressWarnings("unchecked")
        List<TechnicianProfileController.TechnicianSummary> summaries =
                (List<TechnicianProfileController.TechnicianSummary>) model.getAttribute("summaries");
        assertEquals(1, summaries.size());
        TechnicianProfileController.TechnicianSummary s = summaries.get(0);
        assertEquals("tech", s.getTechnician().getEmail());
        assertFalse(s.isClockedIn());
        assertFalse(s.isPaused());
        assertNull(s.getCurrentTaskLabel());
        assertEquals(0L, model.getAttribute("clockedInCount"));
        assertEquals(0L, model.getAttribute("workingCount"));
        assertEquals(0L, model.getAttribute("pausedCount"));
    }

    @Test
    void listTechnicians_manager_populatesClockedInAndActiveTask() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.TECHNICIAN)).thenReturn(List.of(techUser));

        TimeEntry activeEntry = new TimeEntry();
        activeEntry.setUser(techUser);
        activeEntry.setClockInTime(java.time.LocalDateTime.of(2026, 7, 15, 8, 15));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of(activeEntry));

        Task task = new Task();
        task.setTitle("Water clones");
        ChecklistRun run = new ChecklistRun();
        run.setId(42L);
        run.setCompletedBy(techUser);
        run.setTask(task);
        run.setStartTime(java.time.LocalDateTime.of(2026, 7, 15, 9, 0));
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of(run));
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of());

        controller.listTechnicians(session, model);

        @SuppressWarnings("unchecked")
        List<TechnicianProfileController.TechnicianSummary> summaries =
                (List<TechnicianProfileController.TechnicianSummary>) model.getAttribute("summaries");
        TechnicianProfileController.TechnicianSummary s = summaries.get(0);
        assertTrue(s.isClockedIn());
        assertFalse(s.isPaused());
        assertEquals("Water clones", s.getCurrentTaskLabel());
        assertEquals(42L, s.getCurrentRunId());
        assertEquals(1L, model.getAttribute("clockedInCount"));
        assertEquals(1L, model.getAttribute("workingCount"));
        assertEquals(0L, model.getAttribute("pausedCount"));
    }

    @Test
    void listTechnicians_manager_flagsPausedRun() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findByAccountType(AccountType.TECHNICIAN)).thenReturn(List.of(techUser));
        when(timeEntryService.getAllActiveEntries()).thenReturn(List.of());

        Task task = new Task();
        task.setTitle("Trim buds");
        ChecklistRun run = new ChecklistRun();
        run.setId(7L);
        run.setCompletedBy(techUser);
        run.setTask(task);
        when(checklistRunRepository.findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS))
                .thenReturn(List.of(run));
        when(taskPauseRepository.findRunIdsWithOpenPause()).thenReturn(List.of(7L));

        controller.listTechnicians(session, model);

        @SuppressWarnings("unchecked")
        List<TechnicianProfileController.TechnicianSummary> summaries =
                (List<TechnicianProfileController.TechnicianSummary>) model.getAttribute("summaries");
        assertTrue(summaries.get(0).isPaused());
        assertEquals(0L, model.getAttribute("workingCount"));
        assertEquals(1L, model.getAttribute("pausedCount"));
    }

    // --- technicianProfile ---

    @Test
    void technicianProfile_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.technicianProfile(2L, session, model));
    }

    @Test
    void technicianProfile_technician_redirectsToHome() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard", controller.technicianProfile(2L, session, model));
    }

    @Test
    void technicianProfile_managerWithInvalidTechId_redirectsToList() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals("redirect:/technicians", controller.technicianProfile(999L, session, model));
    }

    @Test
    void technicianProfile_managerWithValidTechId_returnsProfile() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(userRepository.findById(2L)).thenReturn(Optional.of(techUser));
        when(checklistRunRepository.findCompletedByUserOrderByStartTimeDesc(techUser))
                .thenReturn(List.of());
        when(timeEntryService.getEntriesForDateRange(any(), any(), any())).thenReturn(List.of());
        when(timeEntryService.isClockedIn(techUser)).thenReturn(false);

        String result = controller.technicianProfile(2L, session, model);

        assertEquals("technician-profile", result);
        assertEquals(techUser, model.getAttribute("technician"));
        assertNotNull(model.getAttribute("completedRuns"));
        assertNotNull(model.getAttribute("timeEntries"));
        assertEquals(false, model.getAttribute("techClockedIn"));
    }
}
