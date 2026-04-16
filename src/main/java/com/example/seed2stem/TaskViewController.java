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
                             @RequestParam(required = false) String description,
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

        ChecklistItem workLogItem = new ChecklistItem();
        workLogItem.setText("Describe the work completed");
        workLogItem.setItemType(ChecklistItemType.QUESTION);
        workLogItem.setResponseType(ChecklistResponseType.TEXT);
        workLogItem.setCategory(ChecklistItemCategory.GENERAL);
        workLogItem.setQuestionOrder(1);
        workLogItem.setDisplayOrder(2);
        workLogItem.setChecklist(checklist);
        checklist.addItem(workLogItem);

        checklistRepo.save(checklist);

        // Create the Task linked to the checklist
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setChecklist(checklist);
        task.setUserCreated(true);
        task.setCreatedBy(user);
        taskRepo.save(task);

        // Create an IN_PROGRESS run — time tracking starts now
        ChecklistRun run = new ChecklistRun();
        run.setTask(task);
        run.setCompletedBy(user);
        run.setStartTime(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.IN_PROGRESS);
        run.setChecklistName(title);
        run.setChecklistVersion(1);
        runRepo.save(run);

        return "redirect:/tasks/runs/" + run.getId() + "/in-progress";
    }

    /* -------------- User Task In-Progress -------------- */

    @GetMapping("/runs/{runId}/in-progress")
    public String userTaskInProgress(@PathVariable Long runId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist run not found"));

        if (!run.getCompletedBy().getId().equals(user.getId())) {
            return "redirect:/dashboard/task-dashboard";
        }

        // Load saved work log text if any
        String savedWorkLog = "";
        if (run.getResponses() != null) {
            for (ChecklistResponse resp : run.getResponses()) {
                if (resp.getTextAnswer() != null) {
                    savedWorkLog = resp.getTextAnswer();
                    break;
                }
            }
        }

        model.addAttribute("run", run);
        model.addAttribute("task", run.getTask());
        model.addAttribute("savedWorkLog", savedWorkLog);
        return "user-task-in-progress";
    }

    @PostMapping("/runs/{runId}/save-progress")
    public String saveProgress(@PathVariable Long runId,
                               @RequestParam String workLog,
                               HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist run not found"));

        if (!run.getCompletedBy().getId().equals(user.getId())) {
            return "redirect:/dashboard/task-dashboard";
        }

        saveWorkLogResponse(run, workLog);
        return "redirect:/tasks/runs/" + runId + "/in-progress";
    }

    @PostMapping("/runs/{runId}/submit-user-task")
    public String submitUserTask(@PathVariable Long runId,
                                 @RequestParam String workLog,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist run not found"));

        if (!run.getCompletedBy().getId().equals(user.getId())) {
            return "redirect:/dashboard/task-dashboard";
        }

        saveWorkLogResponse(run, workLog);
        run.setEndTime(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.PENDING);
        runRepo.save(run);

        return "redirect:/dashboard/task-dashboard";
    }

    private void saveWorkLogResponse(ChecklistRun run, String workLog) {
        Task task = run.getTask();
        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(task.getChecklist().getId());

        ChecklistItem workLogItem = items.stream()
                .filter(i -> i.getItemType() == ChecklistItemType.QUESTION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Work log item not found"));

        // Update existing response or create new one
        if (run.getResponses() != null && !run.getResponses().isEmpty()) {
            ChecklistResponse existing = run.getResponses().stream()
                    .filter(r -> r.getChecklistItem().getId().equals(workLogItem.getId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.setTextAnswer(workLog);
                responseRepo.save(existing);
                return;
            }
        }

        ChecklistResponse response = new ChecklistResponse();
        response.setChecklistRun(run);
        response.setChecklistItem(workLogItem);
        response.setTextAnswer(workLog);
        responseRepo.save(response);
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

        // User-created tasks use the in-progress page, not the standard checklist view
        if (task.isUserCreated()) {
            return "redirect:/tasks/runs/" + runId + "/in-progress";
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
