package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

    @Mock
    private ChecklistRepository checklistRepo;

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
        String result = controller.userCreatedTasks(session, model, false, null, null);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void userCreatedTasks_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model, false, null, null);
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
        when(taskRepo.findByUserCreatedTrueAndDeletedFalse()).thenReturn(List.of(userTask));
        when(runRepo.findFirstByTask(userTask)).thenReturn(Optional.of(run));

        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model, false, null, null);

        assertEquals("user-created-tasks", result);
        assertNotNull(model.getAttribute("userCreatedTasks"));
        assertNotNull(model.getAttribute("taskRunMap"));
    }

    @Test
    void userCreatedTasks_noTasks_returnsEmptyList() {
        session.setAttribute("loggedInUser", developerUser);
        when(taskRepo.findByUserCreatedTrueAndDeletedFalse()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String result = controller.userCreatedTasks(session, model, false, null, null);

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
        req.setEmail("jsmith");
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
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.approveRegistration(1L, "TECHNICIAN", session, redirect);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void approveRegistration_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();
        String result = controller.approveRegistration(1L, "TECHNICIAN", session, redirect);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void approveRegistration_developer_approvesAndRedirects() {
        session.setAttribute("loggedInUser", developerUser);
        User newUser = new User("jsmith", "hashed", "Jane", "Smith", AccountType.TECHNICIAN);
        when(authService.approveRegistration(1L, AccountType.TECHNICIAN)).thenReturn(newUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.approveRegistration(1L, "TECHNICIAN", session, redirect);

        assertEquals("redirect:/developer/registration-requests", result);
        verify(authService).approveRegistration(1L, AccountType.TECHNICIAN);
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
        req.setEmail("jsmith");
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

    // --- createStandardTask (with optional SOP PDF upload) ---

    private static byte[] minimalValidPdf() {
        // Magic header + a few bytes — controller only checks first 4 bytes ("%PDF").
        return new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n'};
    }

    @Test
    void createStandardTask_noSop_savesTaskWithNullSopFields() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.createStandardTask(
                "AM Inspection",
                "Run every morning",
                List.of("HEADER", "QUESTION"),
                List.of("Room 1", "Temperature"),
                List.of("NONE", "NUMBER"), null,
                null, // no file uploaded
                null, // no batch action
                session,
                redirect);

        assertEquals("redirect:/developer/standard-tasks", result);
        verify(checklistRepo).save(any(Checklist.class));
        verify(taskRepo).save(argThat(t ->
                t.getSopFileName() == null && t.getSopData() == null));
    }

    @Test
    void createStandardTask_batchActionWithNoItems_savesBatchTask() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.createStandardTask(
                "Harvest", "Log the harvest", null, null, null, null,
                null, "HARVEST", session, redirect);

        assertEquals("redirect:/developer/standard-tasks", result);
        verify(taskRepo).save(argThat(t -> t.getBatchAction() == BatchAction.HARVEST));
    }

    @Test
    void createStandardTask_noItemsAndNoBatchAction_isRejected() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.createStandardTask(
                "Empty", "desc", null, null, null, null,
                null, "NONE", session, redirect);

        assertEquals("redirect:/developer/standard-tasks/new", result);
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void createStandardTask_validPdf_savesSopBytesOnTask() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        MultipartFile pdf = new MockMultipartFile(
                "sopPdfFile", "sop.pdf", "application/pdf", minimalValidPdf());

        String result = controller.createStandardTask(
                "AM Inspection",
                "desc",
                List.of("QUESTION"),
                List.of("Temp"),
                List.of("NUMBER"), null,
                pdf,
                null, // no batch action
                session,
                redirect);

        assertEquals("redirect:/developer/standard-tasks", result);
        verify(taskRepo).save(argThat(t ->
                "sop.pdf".equals(t.getSopFileName())
                        && t.getSopData() != null
                        && t.getSopData().length == minimalValidPdf().length));
    }

    @Test
    void createStandardTask_nonPdfExtension_redirectsWithError() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        MultipartFile bogus = new MockMultipartFile(
                "sopPdfFile", "evil.exe", "application/pdf", minimalValidPdf());

        String result = controller.createStandardTask(
                "Task", "desc",
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                bogus, null, session, redirect);

        assertEquals("redirect:/developer/standard-tasks/new", result);
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void createStandardTask_wrongContentType_redirectsWithError() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        MultipartFile notPdf = new MockMultipartFile(
                "sopPdfFile", "sop.pdf", "image/png", minimalValidPdf());

        String result = controller.createStandardTask(
                "Task", "desc",
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                notPdf, null, session, redirect);

        assertEquals("redirect:/developer/standard-tasks/new", result);
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void createStandardTask_pdfMissingMagicBytes_redirectsWithError() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        // Right name + content type, but the bytes aren't actually a PDF.
        MultipartFile fake = new MockMultipartFile(
                "sopPdfFile", "sop.pdf", "application/pdf",
                new byte[] {'X', 'X', 'X', 'X', '!'});

        String result = controller.createStandardTask(
                "Task", "desc",
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                fake, null, session, redirect);

        assertEquals("redirect:/developer/standard-tasks/new", result);
        verify(taskRepo, never()).save(any(Task.class));
    }

    @Test
    void createStandardTask_emptyUpload_treatedAsNoSop() {
        session.setAttribute("loggedInUser", developerUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        MultipartFile empty = new MockMultipartFile(
                "sopPdfFile", "", "application/pdf", new byte[0]);

        String result = controller.createStandardTask(
                "Task", "desc",
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                empty, null, session, redirect);

        assertEquals("redirect:/developer/standard-tasks", result);
        verify(taskRepo).save(argThat(t ->
                t.getSopFileName() == null && t.getSopData() == null));
    }

    @Test
    void createStandardTask_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.createStandardTask(
                "Task", "desc",
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                null, null, session, redirect);

        assertEquals("redirect:/dashboard/home-dashboard", result);
        verify(taskRepo, never()).save(any(Task.class));
    }

    // --- edit standard task ---

    private Task existingTask() {
        Checklist checklist = new Checklist(10L, "AM Inspection", 1);
        ChecklistItem header = new ChecklistItem();
        header.setItemType(ChecklistItemType.HEADER);
        header.setResponseType(ChecklistResponseType.NONE);
        header.setText("Room 1");
        header.setChecklist(checklist);
        ChecklistItem question = new ChecklistItem();
        question.setItemType(ChecklistItemType.QUESTION);
        question.setResponseType(ChecklistResponseType.INTEGER);
        question.setText("Temperature");
        question.setChecklist(checklist);
        checklist.addItem(header);
        checklist.addItem(question);

        Task task = new Task("AM Inspection", "Every morning", checklist);
        task.setId(7L);
        task.setSopFileName("old.pdf");
        task.setSopData(new byte[]{'%', 'P', 'D', 'F'});
        return task;
    }

    @Test
    void editStandardTaskForm_developer_prefillsExistingItems() {
        session.setAttribute("loggedInUser", developerUser);
        when(taskRepo.findById(7L)).thenReturn(Optional.of(existingTask()));
        Model model = new ConcurrentModel();

        String view = controller.editStandardTaskForm(7L, session, model, null);

        assertEquals("create-standard-task", view);
        assertNotNull(model.getAttribute("task"));
        List<?> items = (List<?>) model.getAttribute("existingItems");
        assertEquals(2, items.size());
        assertEquals("INTEGER", ((java.util.Map<?, ?>) items.get(1)).get("responseType"));
    }

    @Test
    void editStandardTaskForm_userCreatedTask_redirectsToList() {
        session.setAttribute("loggedInUser", developerUser);
        Task userTask = existingTask();
        userTask.setUserCreated(true);
        when(taskRepo.findById(7L)).thenReturn(Optional.of(userTask));

        assertEquals("redirect:/developer/standard-tasks",
                controller.editStandardTaskForm(7L, session, new ConcurrentModel(), null));
    }

    @Test
    void updateStandardTask_sameItems_keepsChecklistVersion() {
        session.setAttribute("loggedInUser", developerUser);
        Task task = existingTask();
        Checklist original = task.getChecklist();
        when(taskRepo.findById(7L)).thenReturn(Optional.of(task));

        String result = controller.updateStandardTask(7L, "AM Inspection", "Updated description",
                List.of("HEADER", "QUESTION"), List.of("Room 1", "Temperature"), List.of("NONE", "INTEGER"), null,
                null, false, "NONE", session, new RedirectAttributesModelMap());

        assertEquals("redirect:/developer/standard-tasks", result);
        assertSame(original, task.getChecklist());
        assertEquals("Updated description", task.getDescription());
        verify(checklistRepo, never()).save(any());
        verify(taskRepo).save(task);
    }

    @Test
    void updateStandardTask_changedItems_savesNewChecklistVersion() {
        session.setAttribute("loggedInUser", developerUser);
        Task task = existingTask();
        Checklist original = task.getChecklist();
        when(taskRepo.findById(7L)).thenReturn(Optional.of(task));

        controller.updateStandardTask(7L, "AM Inspection v2", null,
                List.of("HEADER", "QUESTION", "QUESTION"),
                List.of("Room 1", "Temperature", "Humidity"),
                List.of("NONE", "INTEGER", "NUMBER"), null,
                null, false, "NONE", session, new RedirectAttributesModelMap());

        assertNotSame(original, task.getChecklist());
        assertEquals(2, task.getChecklist().getVersion());
        assertEquals(3, task.getChecklist().getItems().size());
        assertEquals("AM Inspection v2", task.getTitle());
        assertEquals(2, original.getItems().size()); // old version untouched for past runs
        verify(checklistRepo).save(task.getChecklist());
    }

    @Test
    void updateStandardTask_removeSop_clearsSop() {
        session.setAttribute("loggedInUser", developerUser);
        Task task = existingTask();
        when(taskRepo.findById(7L)).thenReturn(Optional.of(task));

        controller.updateStandardTask(7L, "AM Inspection", null,
                List.of("HEADER", "QUESTION"), List.of("Room 1", "Temperature"), List.of("NONE", "INTEGER"), null,
                null, true, "NONE", session, new RedirectAttributesModelMap());

        assertNull(task.getSopFileName());
        assertNull(task.getSopData());
    }

    @Test
    void updateStandardTask_blankTitle_returnsToEditWithError() {
        session.setAttribute("loggedInUser", developerUser);
        when(taskRepo.findById(7L)).thenReturn(Optional.of(existingTask()));

        String result = controller.updateStandardTask(7L, " ", null,
                List.of("QUESTION"), List.of("Q"), List.of("TEXT"), null,
                null, false, "NONE", session, new RedirectAttributesModelMap());

        assertEquals("redirect:/developer/standard-tasks/7/edit", result);
        verify(taskRepo, never()).save(any());
    }

    @Test
    void updateStandardTask_nonDeveloper_redirectsToDashboard() {
        session.setAttribute("loggedInUser", techUser);

        String result = controller.updateStandardTask(7L, "x", null, null, null, null, null,
                null, false, null, session, new RedirectAttributesModelMap());

        assertEquals("redirect:/dashboard/home-dashboard", result);
        verify(taskRepo, never()).save(any());
    }
}
