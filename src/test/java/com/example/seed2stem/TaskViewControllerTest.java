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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskViewControllerTest {

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private ChecklistItemRepository itemRepo;

    @Mock
    private ChecklistRunRepository runRepo;

    @Mock
    private ChecklistRunService runService;

    @Mock
    private ChecklistRepository checklistRepo;

    @Mock
    private ChecklistResponseRepository responseRepo;

    @InjectMocks
    private TaskViewController controller;

    private MockHttpSession session;
    private User techUser;
    private Task task;
    private Checklist checklist;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(1L);

        checklist = new Checklist(1L, "AM Inspection", 1);

        task = new Task();
        task.setId(1L);
        task.setTitle("Morning Check");
        task.setChecklist(checklist);
    }

    // --- createTaskForm ---

    @Test
    void createTaskForm_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.createTaskForm(session, model);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void createTaskForm_loggedIn_returnsCreateTaskView() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        String result = controller.createTaskForm(session, model);
        assertEquals("create-task", result);
    }

    // --- createTask POST ---

    @Test
    void createTask_noUser_redirectsToLogin() {
        String result = controller.createTask("Test", "Desc", session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void createTask_validInput_createsTaskChecklistAndRun() {
        session.setAttribute("loggedInUser", techUser);
        when(checklistRepo.save(any(Checklist.class))).thenAnswer(inv -> {
            Checklist cl = inv.getArgument(0);
            cl.setId(10L);
            // Set IDs on items for the response to reference
            List<ChecklistItem> items = cl.getItems();
            if (items.size() >= 2) {
                items.get(1).setId(20L);
            }
            return cl;
        });
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });
        when(runRepo.save(any(ChecklistRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepo.save(any(ChecklistResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = controller.createTask("Clean Reservoirs", "Cleaned all reservoirs in B2", session);

        assertEquals("redirect:/dashboard/task-dashboard", result);

        verify(checklistRepo).save(argThat(cl -> {
            assertEquals("Clean Reservoirs", cl.getName());
            assertEquals(2, cl.getItems().size());
            assertEquals(ChecklistItemType.HEADER, cl.getItems().get(0).getItemType());
            assertEquals(ChecklistItemType.QUESTION, cl.getItems().get(1).getItemType());
            assertEquals(ChecklistResponseType.TEXT, cl.getItems().get(1).getResponseType());
            return true;
        }));

        verify(taskRepo).save(argThat(t -> {
            assertEquals("Clean Reservoirs", t.getTitle());
            assertEquals("Cleaned all reservoirs in B2", t.getDescription());
            assertTrue(t.isUserCreated());
            assertEquals(techUser, t.getCreatedBy());
            return true;
        }));

        verify(runRepo).save(argThat(run -> {
            assertEquals(ChecklistRunStatus.PENDING, run.getStatus());
            assertEquals(techUser, run.getCompletedBy());
            assertNotNull(run.getStartTime());
            assertNotNull(run.getEndTime());
            return true;
        }));

        verify(responseRepo).save(argThat(resp -> {
            assertEquals("Cleaned all reservoirs in B2", resp.getTextAnswer());
            return true;
        }));
    }

    // --- viewTask ---

    @Test
    void viewTask_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.viewTask(1L, model, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void viewTask_validTask_returnsTaskView() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Model model = new ConcurrentModel();

        String result = controller.viewTask(1L, model, session);

        assertEquals("task-view", result);
        assertEquals(task, model.getAttribute("task"));
    }

    @Test
    void viewTask_invalidTaskId_throwsException() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(999L)).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        assertThrows(RuntimeException.class, () -> controller.viewTask(999L, model, session));
    }

    // --- startTask ---

    @Test
    void startTask_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.startTask(1L, model, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void startTask_existingInProgressRun_redirectsToResume() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        ChecklistRun existingRun = new ChecklistRun();
        existingRun.setStatus(ChecklistRunStatus.IN_PROGRESS);
        when(runService.findExistingInProgressRun(techUser, task)).thenReturn(Optional.of(existingRun));

        Model model = new ConcurrentModel();
        String result = controller.startTask(1L, model, session);

        assertTrue(result.startsWith("redirect:/tasks/1/resume/"));
    }

    @Test
    void startTask_noExistingRun_createsNewRunAndReturnsChecklistView() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(runService.findExistingInProgressRun(techUser, task)).thenReturn(Optional.empty());
        when(runRepo.save(any(ChecklistRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String result = controller.startTask(1L, model, session);

        assertEquals("checklist-view", result);
        assertEquals(task, model.getAttribute("task"));
        assertEquals(checklist, model.getAttribute("checklist"));
        assertNotNull(model.getAttribute("items"));
        assertNotNull(model.getAttribute("responses"));
        verify(runRepo).save(any(ChecklistRun.class));
    }

    @Test
    void startTask_invalidTaskId_throwsException() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(999L)).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        assertThrows(RuntimeException.class, () -> controller.startTask(999L, model, session));
    }

    // --- resumeTask ---

    @Test
    void resumeTask_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.resumeTask(1L, 1L, model, session);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void resumeTask_validRun_returnsChecklistView() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistRun run = new ChecklistRun();
        run.setCompletedBy(techUser);
        run.setResponses(List.of());

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String result = controller.resumeTask(1L, 1L, model, session);

        assertEquals("checklist-view", result);
        assertEquals(task, model.getAttribute("task"));
        assertEquals(checklist, model.getAttribute("checklist"));
    }

    @Test
    void resumeTask_wrongUser_redirectsToTaskDashboard() {
        session.setAttribute("loggedInUser", techUser);

        User otherUser = new User("other", "h", "Other", "User", AccountType.TECHNICIAN);
        otherUser.setId(99L);

        ChecklistRun run = new ChecklistRun();
        run.setCompletedBy(otherUser);

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));

        Model model = new ConcurrentModel();
        String result = controller.resumeTask(1L, 1L, model, session);

        assertEquals("redirect:/dashboard/task-dashboard", result);
    }

    @Test
    void resumeTask_invalidTaskId_throwsException() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(999L)).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        assertThrows(RuntimeException.class, () -> controller.resumeTask(999L, 1L, model, session));
    }

    @Test
    void resumeTask_invalidRunId_throwsException() {
        session.setAttribute("loggedInUser", techUser);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(runRepo.findById(999L)).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        assertThrows(RuntimeException.class, () -> controller.resumeTask(1L, 999L, model, session));
    }

    @Test
    void resumeTask_withExistingResponses_populatesResponseMap() {
        session.setAttribute("loggedInUser", techUser);

        ChecklistItem item = new ChecklistItem();
        item.setId(10L);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);

        ChecklistResponse resp = new ChecklistResponse();
        resp.setChecklistItem(item);
        resp.setBooleanAnswer(true);

        ChecklistRun run = new ChecklistRun();
        run.setCompletedBy(techUser);
        run.setResponses(List.of(resp));

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(itemRepo.findByChecklistIdOrderByDisplayOrder(1L)).thenReturn(List.of(item));

        Model model = new ConcurrentModel();
        String result = controller.resumeTask(1L, 1L, model, session);

        assertEquals("checklist-view", result);
        @SuppressWarnings("unchecked")
        java.util.Map<Long, ChecklistResponse> responses =
                (java.util.Map<Long, ChecklistResponse>) model.getAttribute("responses");
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey(10L));
    }
}
