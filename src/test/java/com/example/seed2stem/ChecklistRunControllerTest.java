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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistRunControllerTest {

    @Mock
    private ChecklistRunService runService;

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private ChecklistItemRepository itemRepo;

    @Mock
    private ChecklistRunRepository runRepo;

    @Mock
    private TaskPauseRepository taskPauseRepo;

    @InjectMocks
    private ChecklistRunController controller;

    private MockHttpSession session;
    private User techUser;
    private User managerUser;
    private Task task;
    private Checklist checklist;
    private ChecklistRun run;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(1L);
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(2L);

        checklist = new Checklist(1L, "AM Inspection", 1);

        task = new Task();
        task.setId(1L);
        task.setTitle("Morning Check");
        task.setChecklist(checklist);

        run = new ChecklistRun();
        run.setStatus(ChecklistRunStatus.IN_PROGRESS);
        run.setCompletedBy(techUser);
        run.setTask(task);
        run.setStartTime(LocalDateTime.now());
    }

    // --- submitChecklist ---

    @Test
    void submitChecklist_noUser_redirectsToLogin() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("taskId", "1");
        params.add("runId", "1");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void submitChecklist_validSubmission_redirectsToTaskDashboard() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.submitWithResponses(eq(1L), anyList())).thenReturn(run);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("taskId", "1");
        params.add("runId", "1");
        params.add("bool_10", "true");
        params.add("text_10", "Looks good");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).submitWithResponses(eq(1L), anyList());
    }

    @Test
    void submitChecklist_missingBooleanAnswer_redirectsToResume() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);
        item.setText("Is temperature OK?");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // No bool_10 param
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/tasks/1/resume/1", result);
    }

    @Test
    void submitChecklist_invalidNumber_redirectsToResume() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.INTEGER);
        item.setText("Plant count");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("num_10", "not-a-number");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/tasks/1/resume/1", result);
    }

    @Test
    void submitChecklist_missingNumericValue_redirectsToResume() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.NUMBER);
        item.setText("Temperature");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // No num_10 param
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/tasks/1/resume/1", result);
    }

    @Test
    void submitChecklist_textResponse_succeeds() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.submitWithResponses(eq(1L), anyList())).thenReturn(run);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("text_10", "Some notes");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/dashboard/task-dashboard", result);
    }

    @Test
    void submitChecklist_headerItemsSkipped() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem header = new ChecklistItem();
        header.setId(1L);
        header.setItemType(ChecklistItemType.HEADER);
        header.setResponseType(ChecklistResponseType.NONE);

        ChecklistItem question = new ChecklistItem();
        question.setId(10L);
        question.setItemType(ChecklistItemType.QUESTION);
        question.setResponseType(ChecklistResponseType.TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(header, question));
        when(runService.submitWithResponses(eq(1L), anyList())).thenReturn(run);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("text_10", "answer");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/dashboard/task-dashboard", result);
    }

    @Test
    void submitChecklist_noneTypeUnchecked_redirectsToResume() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.NONE);
        item.setText("Wash hands");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // No check_10 — must trigger validation redirect
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/tasks/1/resume/1", result);
    }

    @Test
    void submitChecklist_noneTypeChecked_succeeds() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.NONE);
        item.setText("Wash hands");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.submitWithResponses(eq(1L), anyList())).thenReturn(run);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("check_10", "true");
        var redirect = new RedirectAttributesModelMap();

        String result = controller.submitChecklist(1L, 1L, params, session, redirect);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).submitWithResponses(eq(1L), argThat(list ->
                list.size() == 1
                        && Boolean.TRUE.equals(list.get(0).getBooleanAnswer())));
    }

    // --- pauseChecklist ---

    @Test
    void pauseChecklist_noUser_redirectsToLogin() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        String result = controller.pauseChecklist(1L, 1L, null, params, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void pauseChecklist_validPause_redirectsToTaskDashboard() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("bool_10", "true");
        params.add("text_10", "partial notes");

        String result = controller.pauseChecklist(1L, 1L, "Lunch break", params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).pauseByUser(eq(1L), anyList());
        verify(taskPauseRepo).save(argThat(p ->
                "Lunch break".equals(p.getReason()) && p.getStartTime() != null));
    }

    @Test
    void pauseChecklist_blankReason_savesPauseWithNullReason() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        String result = controller.pauseChecklist(1L, 1L, "   ", params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(taskPauseRepo).save(argThat(p -> p.getReason() == null));
    }

    @Test
    void pauseChecklist_openPauseExists_doesNotCreateDuplicate() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        TaskPause existing = new TaskPause();
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.of(existing));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        String result = controller.pauseChecklist(1L, 1L, "Reason", params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(taskPauseRepo, never()).save(any(TaskPause.class));
    }

    @Test
    void pauseChecklist_missingBooleanIsNull_handledGracefully() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // No bool_10 — should be null, not throw

        String result = controller.pauseChecklist(1L, 1L, null, params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
    }

    @Test
    void pauseChecklist_invalidNumber_ignoredSilently() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.INTEGER);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("num_10", "not-a-number");

        String result = controller.pauseChecklist(1L, 1L, null, params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
    }

    @Test
    void pauseChecklist_noneTypeChecked_savesBooleanTrue() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.NONE);
        item.setText("Wash hands");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("check_10", "true");

        String result = controller.pauseChecklist(1L, 1L, null, params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).pauseByUser(eq(1L), argThat(list ->
                list.size() == 1
                        && Boolean.TRUE.equals(list.get(0).getBooleanAnswer())));
    }

    @Test
    void pauseChecklist_noneTypeUnchecked_doesNotPersistResponse() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.NONE);
        item.setText("Wash hands");

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));
        when(runService.pauseByUser(eq(1L), anyList())).thenReturn(run);
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run))
                .thenReturn(Optional.empty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // No check_10 — pause must not persist a response for this item

        String result = controller.pauseChecklist(1L, 1L, null, params, session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).pauseByUser(eq(1L), argThat(List::isEmpty));
    }

    // --- reviewChecklistRun ---

    @Test
    void reviewChecklistRun_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.reviewChecklistRun(1L, model, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void reviewChecklistRun_nullRun_redirectsToHome() {
        session.setAttribute("loggedInUser", managerUser);
        when(runService.getChecklistByIdWithDetails(1L)).thenReturn(null);
        Model model = new ConcurrentModel();

        String result = controller.reviewChecklistRun(1L, model, session);
        assertEquals("redirect:/dashboard/home-dashboard", result);
    }

    @Test
    void reviewChecklistRun_validRun_returnsReviewView() {
        session.setAttribute("loggedInUser", managerUser);

        ChecklistItem header = new ChecklistItem();
        header.setId(1L);
        header.setItemType(ChecklistItemType.HEADER);
        header.setResponseType(ChecklistResponseType.NONE);
        header.setText("Section 1");

        ChecklistItem question = new ChecklistItem();
        question.setId(10L);
        question.setItemType(ChecklistItemType.QUESTION);
        question.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);
        question.setText("Is temp OK?");

        checklist.setItems(List.of(header, question));

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(question);
        resp.setBooleanAnswer(true);

        run.setStatus(ChecklistRunStatus.PENDING);
        run.setResponses(List.of(resp));

        when(runService.getChecklistByIdWithDetails(1L)).thenReturn(run);
        Model model = new ConcurrentModel();

        String result = controller.reviewChecklistRun(1L, model, session);
        assertEquals("checklist-review-view", result);
        assertNotNull(model.getAttribute("run"));
        assertNotNull(model.getAttribute("task"));
        assertNotNull(model.getAttribute("responsesByHeader"));
        assertNotNull(model.getAttribute("headers"));
    }

    // --- approveChecklistRun ---

    @Test
    void approveChecklistRun_noUser_redirectsToLogin() {
        String result = controller.approveChecklistRun(1L, "Good", session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void approveChecklistRun_manager_approvesAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        when(runService.authorizeByManager(1L, managerUser, "Approved")).thenReturn(run);

        String result = controller.approveChecklistRun(1L, "Approved", session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).authorizeByManager(1L, managerUser, "Approved");
    }

    // --- rejectChecklistRun ---

    @Test
    void rejectChecklistRun_noUser_redirectsToLogin() {
        String result = controller.rejectChecklistRun(1L, "Redo", session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void rejectChecklistRun_manager_rejectsAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        when(runService.rejectByManager(1L, managerUser, "Redo section 3")).thenReturn(run);

        String result = controller.rejectChecklistRun(1L, "Redo section 3", session);
        assertEquals("redirect:/dashboard/task-dashboard", result);
        verify(runService).rejectByManager(1L, managerUser, "Redo section 3");
    }

    // --- getPendingChecklists ---

    @Test
    void getPendingChecklists_returnsPendingList() {
        ChecklistRun pendingRun = new ChecklistRun();
        pendingRun.setStatus(ChecklistRunStatus.PENDING);
        when(runService.getPendingChecklists()).thenReturn(List.of(pendingRun));

        List<ChecklistRun> result = controller.getPendingChecklists();
        assertEquals(1, result.size());
        assertEquals(ChecklistRunStatus.PENDING, result.get(0).getStatus());
    }
}
