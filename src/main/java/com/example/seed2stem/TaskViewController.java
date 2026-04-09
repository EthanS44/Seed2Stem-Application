package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    private final TaskRepository taskRepo;
    private final ChecklistItemRepository itemRepo;
    private final ChecklistRunRepository runRepo;
    private final ChecklistRunService runService;
    private final ChecklistRepository checklistRepo;
    private final ChecklistResponseRepository responseRepo;

    public TaskViewController(TaskRepository taskRepo,
                              ChecklistItemRepository itemRepo,
                              ChecklistRunRepository runRepo,
                              ChecklistRunService runService,
                              ChecklistRepository checklistRepo,
                              ChecklistResponseRepository responseRepo) {
        this.taskRepo = taskRepo;
        this.itemRepo = itemRepo;
        this.runRepo = runRepo;
        this.runService = runService;
        this.checklistRepo = checklistRepo;
        this.responseRepo = responseRepo;
    }

    /* ---------------- Create Task ---------------- */

    @GetMapping("/create")
    public String createTaskForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        return "create-task";
    }

    @PostMapping("/create")
    public String createTask(@RequestParam String title,
                             @RequestParam String description,
                             HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // Build a Checklist with a header and a text question
        Checklist checklist = new Checklist();
        checklist.setName(title);
        checklist.setVersion(1);

        ChecklistItem header = new ChecklistItem();
        header.setText(title);
        header.setItemType(ChecklistItemType.HEADER);
        header.setResponseType(ChecklistResponseType.NONE);
        header.setDisplayOrder(1);
        header.setChecklist(checklist);
        checklist.addItem(header);

        ChecklistItem descriptionItem = new ChecklistItem();
        descriptionItem.setText("Description of work completed");
        descriptionItem.setItemType(ChecklistItemType.QUESTION);
        descriptionItem.setResponseType(ChecklistResponseType.TEXT);
        descriptionItem.setCategory(ChecklistItemCategory.GENERAL);
        descriptionItem.setQuestionOrder(1);
        descriptionItem.setDisplayOrder(2);
        descriptionItem.setChecklist(checklist);
        checklist.addItem(descriptionItem);

        checklistRepo.save(checklist);

        // Create the Task linked to the checklist
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setChecklist(checklist);
        task.setUserCreated(true);
        task.setCreatedBy(user);
        taskRepo.save(task);

        // Create a ChecklistRun with PENDING status and pre-filled response
        ChecklistRun run = new ChecklistRun();
        run.setTask(task);
        run.setCompletedBy(user);
        run.setStartTime(LocalDateTime.now());
        run.setEndTime(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.PENDING);
        run.setChecklistName(title);
        run.setChecklistVersion(1);
        runRepo.save(run);

        // Pre-fill the description response
        ChecklistResponse response = new ChecklistResponse();
        response.setChecklistRun(run);
        response.setChecklistItem(descriptionItem);
        response.setTextAnswer(description);
        responseRepo.save(response);

        return "redirect:/dashboard/task-dashboard";
    }

    /* ---------------- Task View ---------------- */

    @GetMapping("/{taskId}")
    public String viewTask(@PathVariable Long taskId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        model.addAttribute("task", task);
        return "task-view";
    }

    /* -------------- Start Task (Checklist) -------------- */

    @GetMapping("/{taskId}/start")
    public String startTask(@PathVariable Long taskId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Check for existing in-progress run — redirect to resume if found
        Optional<ChecklistRun> existingRun = runService.findExistingInProgressRun(user, task);
        if (existingRun.isPresent()) {
            return "redirect:/tasks/" + taskId + "/resume/" + existingRun.get().getId();
        }

        // Create new ChecklistRun
        ChecklistRun run = new ChecklistRun();
        run.setTask(task);
        run.setCompletedBy(user);
        run.setStartTime(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.IN_PROGRESS);

        Checklist cl = task.getChecklist();
        run.setChecklistName(cl.getName());
        run.setChecklistVersion(cl.getVersion());

        runRepo.save(run);

        model.addAttribute("task", task);
        model.addAttribute("checklist", cl);
        model.addAttribute("runId", run.getId());
        model.addAttribute("items", itemRepo.findByChecklistIdOrderByDisplayOrder(cl.getId()));
        model.addAttribute("responses", new HashMap<Long, ChecklistResponse>());

        return "checklist-view";
    }

    /* -------------- Resume Task (Checklist) -------------- */

    @GetMapping("/{taskId}/resume/{runId}")
    public String resumeTask(@PathVariable Long taskId, @PathVariable Long runId,
                             Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist run not found"));

        // Security check: ensure this run belongs to the current user
        if (!run.getCompletedBy().getId().equals(user.getId())) {
            return "redirect:/dashboard/task-dashboard";
        }

        Checklist cl = task.getChecklist();
        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(cl.getId());

        // Build map of checklistItemId -> ChecklistResponse for pre-filling
        Map<Long, ChecklistResponse> responseMap = new HashMap<>();
        if (run.getResponses() != null) {
            for (ChecklistResponse resp : run.getResponses()) {
                responseMap.put(resp.getChecklistItem().getId(), resp);
            }
        }

        model.addAttribute("task", task);
        model.addAttribute("checklist", cl);
        model.addAttribute("runId", run.getId());
        model.addAttribute("items", items);
        model.addAttribute("responses", responseMap);

        return "checklist-view";
    }
}
