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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeveloperControllerTest {

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private ChecklistRunRepository runRepo;

    @Mock
    private RegistrationRequestRepository registrationRequestRepo;

    @Mock
    private PasswordResetRequestRepository passwordResetRequestRepo;

    @Mock
    private AuthService authService;

    @InjectMocks
    private DeveloperController controller;

    private MockHttpSession session;
    private User developerUser;
    private User techUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        developerUser = new User("dev", "hashed", "Dev", "User", AccountType.DEVELOPER);
        developerUser.setId(1L);
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(2L);
    }

    // --- userCreatedTasks ---

    @Test
    void userCreatedTasks_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void userCreatedTasks_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void userCreatedTasks_developer_returnsViewWithTasks() {
        session.setAttribute("loggedInUser", developerUser);

        Task userTask = new Task();
        userTask.setId(10L);
        userTask.setTitle("Custom Task");
        userTask.setUserCreated(true);
        userTask.setCreatedBy(techUser);

        ChecklistRun run = new ChecklistRun();
        run.setTask(userTask);
        // Use reflection-free approach
        when(taskRepo.findByUserCreatedTrue()).thenReturn(List.of(userTask));
        when(runRepo.findFirstByTask(userTask)).thenReturn(Optional.of(run));

        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model);

        assertEquals("user-created-tasks", result);
        assertNotNull(model.getAttribute("userCreatedTasks"));
        assertNotNull(model.getAttribute("taskRunMap"));
    }

    @Test
    void userCreatedTasks_noTasks_returnsEmptyList() {
        session.setAttribute("loggedInUser", developerUser);
        when(taskRepo.findByUserCreatedTrue()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model);

        assertEquals("user-created-tasks", result);
        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) model.getAttribute("userCreatedTasks");
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    // --- registrationRequests ---

    @Test
    void registrationRequests_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.registrationRequests(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void registrationRequests_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        String result = controller.registrationRequests(session, model);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void registrationRequests_developer_returnsViewWithPendingRequests() {
        session.setAttribute("loggedInUser", developerUser);

        RegistrationRequest req = new RegistrationRequest();
        req.setId(1L);
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setUsername("jsmith");
        req.setAccountType(AccountType.TECHNICIAN);
        req.setStatus(RegistrationStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());

        when(registrationRequestRepo.findByStatus(RegistrationStatus.PENDING))
                .thenReturn(List.of(req));

        Model model = new ConcurrentModel();
        String result = controller.registrationRequests(session, model);

        assertEquals("registration-requests", result);
        assertNotNull(model.getAttribute("pendingRequests"));
    }

    // --- approveRegistration ---

    @Test
    void approveRegistration_noUser_redirectsToLogin() {
        String result = controller.approveRegistration(1L, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void approveRegistration_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        String result = controller.approveRegistration(1L, session);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void approveRegistration_developer_approvesAndRedirects() {
        session.setAttribute("loggedInUser", developerUser);
        User newUser = new User("jsmith", "hashed", "Jane", "Smith", AccountType.TECHNICIAN);
        when(authService.approveRegistration(1L)).thenReturn(newUser);

        String result = controller.approveRegistration(1L, session);

        assertEquals("redirect:/developer/registration-requests", result);
        verify(authService).approveRegistration(1L);
    }

    // --- denyRegistration ---

    @Test
    void denyRegistration_noUser_redirectsToLogin() {
        String result = controller.denyRegistration(1L, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void denyRegistration_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        String result = controller.denyRegistration(1L, session);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void denyRegistration_developer_deniesAndRedirects() {
        session.setAttribute("loggedInUser", developerUser);
        doNothing().when(authService).denyRegistration(1L);

        String result = controller.denyRegistration(1L, session);

        assertEquals("redirect:/developer/registration-requests", result);
        verify(authService).denyRegistration(1L);
    }

    // --- passwordResetRequests ---

    @Test
    void passwordResetRequests_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.passwordResetRequests(session, model, null, null);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void passwordResetRequests_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        String result = controller.passwordResetRequests(session, model, null, null);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void passwordResetRequests_developer_returnsViewWithPendingRequests() {
        session.setAttribute("loggedInUser", developerUser);

        PasswordResetRequest req = new PasswordResetRequest();
        req.setId(1L);
        req.setUsername("jsmith");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setStatus(PasswordResetStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());

        when(passwordResetRequestRepo.findByStatus(PasswordResetStatus.PENDING))
                .thenReturn(List.of(req));

        Model model = new ConcurrentModel();
        String result = controller.passwordResetRequests(session, model, null, null);

        assertEquals("password-reset-requests", result);
        assertNotNull(model.getAttribute("pendingRequests"));
    }

    // --- approvePasswordReset ---

    @Test
    void approvePasswordReset_noUser_redirectsToLogin() {
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.approvePasswordReset(1L, "newpass", session, redirect);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void approvePasswordReset_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.approvePasswordReset(1L, "newpass", session, redirect);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void approvePasswordReset_developer_approvesAndRedirects() {
        session.setAttribute("loggedInUser", developerUser);
        doNothing().when(authService).approvePasswordReset(1L, "newpass", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.approvePasswordReset(1L, "newpass", session, redirect);

        assertEquals("redirect:/developer/password-reset-requests", result);
        verify(authService).approvePasswordReset(1L, "newpass", developerUser);
    }

    @Test
    void approvePasswordReset_serviceThrows_redirectsWithError() {
        session.setAttribute("loggedInUser", developerUser);
        doThrow(new RuntimeException("New password cannot be blank"))
                .when(authService).approvePasswordReset(1L, "", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.approvePasswordReset(1L, "", session, redirect);

        assertEquals("redirect:/developer/password-reset-requests", result);
    }

    // --- denyPasswordReset ---

    @Test
    void denyPasswordReset_noUser_redirectsToLogin() {
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.denyPasswordReset(1L, session, redirect);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void denyPasswordReset_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.denyPasswordReset(1L, session, redirect);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void denyPasswordReset_developer_deniesAndRedirects() {
        session.setAttribute("loggedInUser", developerUser);
        doNothing().when(authService).denyPasswordReset(1L, developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.denyPasswordReset(1L, session, redirect);

        assertEquals("redirect:/developer/password-reset-requests", result);
        verify(authService).denyPasswordReset(1L, developerUser);
    }
}
