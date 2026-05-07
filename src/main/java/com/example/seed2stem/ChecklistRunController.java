package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/checklist-runs")
public class ChecklistRunController {

    private static final Logger log = LoggerFactory.getLogger(ChecklistRunController.class);

    private final ChecklistRunService runService;
    private final TaskRepository taskRepo;
    private final ChecklistItemRepository itemRepo;
    private final ChecklistRunRepository runRepo;
    private final TaskPauseRepository taskPauseRepo;

    public ChecklistRunController(ChecklistRunService runService,
                                  TaskRepository taskRepo,
                                  ChecklistItemRepository itemRepo,
                                  ChecklistRunRepository runRepo,
                                  TaskPauseRepository taskPauseRepo) {
        this.runService = runService;
        this.taskRepo = taskRepo;
        this.itemRepo = itemRepo;
        this.runRepo = runRepo;
        this.taskPauseRepo = taskPauseRepo;
    }

    @PostMapping("/submit")
    public String submitChecklist(@RequestParam Long taskId,
                                  @RequestParam Long runId,
                                  @RequestParam MultiValueMap<String, String> params,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId).orElseThrow();

        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(
                task.getChecklist().getId());

        List<ChecklistResponse> responses = new ArrayList<>();

        for (ChecklistItem item : items) {
            if (item.getItemType() != ChecklistItemType.QUESTION) continue;

            ChecklistResponse resp = new ChecklistResponse();
            resp.setChecklistItem(item);

            switch (item.getResponseType()) {
                case BOOLEAN_TEXT -> {
                    String boolVal = params.getFirst("bool_" + item.getId());
                    if (boolVal == null) {
                        redirectAttributes.addFlashAttribute("error",
                                "Missing answer for: " + item.getText());
                        return "redirect:/tasks/" + taskId + "/resume/" + runId;
                    }
                    resp.setBooleanAnswer(Boolean.valueOf(boolVal));
                    resp.setTextAnswer(params.getFirst("text_" + item.getId()));
                }
                case TEXT -> {
                    resp.setTextAnswer(params.getFirst("text_" + item.getId()));
                }
                case INTEGER, NUMBER, DECIMAL -> {
                    String numVal = params.getFirst("num_" + item.getId());
                    if (numVal == null || numVal.isBlank()) {
                        redirectAttributes.addFlashAttribute("error",
                                "Missing numeric value for: " + item.getText());
                        return "redirect:/tasks/" + taskId + "/resume/" + runId;
                    }
                    try {
                        resp.setNumericAnswer(Double.valueOf(numVal));
                    } catch (NumberFormatException e) {
                        redirectAttributes.addFlashAttribute("error",
                                "Invalid number for: " + item.getText());
                        return "redirect:/tasks/" + taskId + "/resume/" + runId;
                    }
                }
                case NONE -> {
                    // Checkbox items: must be checked to submit.
                    if (!params.containsKey("check_" + item.getId())) {
                        redirectAttributes.addFlashAttribute("error",
                                "Please check: " + item.getText());
                        return "redirect:/tasks/" + taskId + "/resume/" + runId;
                    }
                    resp.setBooleanAnswer(true);
                }
            }

            responses.add(resp);
        }

        runService.submitWithResponses(runId, responses);

        // Close any open pause so timeline calc doesn't see it as still paused
        runRepo.findById(runId).ifPresent(run ->
                taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run)
                        .ifPresent(p -> {
                            p.setEndTime(LocalDateTime.now());
                            taskPauseRepo.save(p);
                        }));

        return "redirect:/dashboard/task-dashboard";
    }

    @PostMapping("/pause")
    public String pauseChecklist(@RequestParam Long taskId,
                                 @RequestParam Long runId,
                                 @RequestParam(required = false) String pauseReason,
                                 @RequestParam MultiValueMap<String, String> params,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId).orElseThrow();

        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(
                task.getChecklist().getId());

        List<ChecklistResponse> responses = new ArrayList<>();

        for (ChecklistItem item : items) {
            if (item.getItemType() != ChecklistItemType.QUESTION) continue;

            ChecklistResponse resp = new ChecklistResponse();
            resp.setChecklistItem(item);

            // Whether this response carries any data worth saving for resume.
            boolean keep = true;

            switch (item.getResponseType()) {
                case BOOLEAN_TEXT -> {
                    String boolVal = params.getFirst("bool_" + item.getId());
                    if (boolVal != null) {
                        resp.setBooleanAnswer(Boolean.valueOf(boolVal));
                    }
                    resp.setTextAnswer(params.getFirst("text_" + item.getId()));
                }
                case TEXT -> {
                    resp.setTextAnswer(params.getFirst("text_" + item.getId()));
                }
                case INTEGER, NUMBER, DECIMAL -> {
                    String numVal = params.getFirst("num_" + item.getId());
                    if (numVal != null && !numVal.isBlank()) {
                        try {
                            resp.setNumericAnswer(Double.valueOf(numVal));
                        } catch (NumberFormatException e) {
                            // Silently ignore invalid numbers on pause
                        }
                    }
                }
                case NONE -> {
                    // Checkbox items on pause: only persist when checked.
                    if (params.containsKey("check_" + item.getId())) {
                        resp.setBooleanAnswer(true);
                    } else {
                        keep = false;
                    }
                }
            }

            if (keep) responses.add(resp);
        }

        runService.pauseByUser(runId, responses);

        // Record the pause — only if there isn't already one open (guard against double-submit)
        runRepo.findById(runId).ifPresent(run -> {
            if (taskPauseRepo.findFirstByChecklistRunAndEndTimeIsNullOrderByStartTimeDesc(run).isEmpty()) {
                TaskPause pause = new TaskPause();
                pause.setChecklistRun(run);
                pause.setStartTime(LocalDateTime.now());
                if (pauseReason != null && !pauseReason.trim().isEmpty()) {
                    pause.setReason(pauseReason.trim());
                }
                taskPauseRepo.save(pause);
            }
        });

        return "redirect:/dashboard/task-dashboard";
    }

    @GetMapping("/runs/{runId}")
    public String reviewChecklistRun(@PathVariable Long runId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        ChecklistRun run = runService.getChecklistByIdWithDetails(runId);
        if (run == null) {
            log.warn("ChecklistRun not found for id: {}", runId);
            return "redirect:/dashboard/home-dashboard";
        }

        if (run.getTask() == null || run.getTask().getChecklist() == null) {
            log.warn("ChecklistRun {} has null task or checklist", runId);
            return "redirect:/dashboard/home-dashboard";
        }

        List<ChecklistItem> items = run.getTask().getChecklist().getItems();
        List<ChecklistResponse> responses = run.getResponses() != null ? run.getResponses() : List.of();

        // Use merge function to handle potential duplicate responses for the same item
        Map<Long, ChecklistResponse> responseMap = responses.stream()
                .filter(r -> r.getChecklistItem() != null)
                .collect(Collectors.toMap(
                        r -> r.getChecklistItem().getId(),
                        r -> r,
                        (existing, replacement) -> replacement));

        Map<Long, List<ChecklistResponse>> responsesByHeader = new LinkedHashMap<>();
        ChecklistItem currentHeader = null;

        for (ChecklistItem item : items) {
            if (item.getItemType() == ChecklistItemType.HEADER) {
                currentHeader = item;
                responsesByHeader.put(item.getId(), new ArrayList<>());
            } else if (currentHeader != null) {
                ChecklistResponse r = responseMap.get(item.getId());
                if (r != null) {
                    responsesByHeader.get(currentHeader.getId()).add(r);
                }
            }
        }

        model.addAttribute("run", run);
        model.addAttribute("task", run.getTask());
        model.addAttribute("responsesByHeader", responsesByHeader);
        model.addAttribute("headers", items.stream()
                .filter(i -> i.getItemType() == ChecklistItemType.HEADER)
                .toList()
        );

        return "checklist-review-view";
    }

    @PostMapping("/runs/{runId}/approve")
    public String approveChecklistRun(
            @PathVariable Long runId,
            @RequestParam("managerComments") String managerComments,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        runService.authorizeByManager(runId, user, managerComments);
        return "redirect:/dashboard/task-dashboard";
    }

    @PostMapping("/runs/{runId}/reject")
    public String rejectChecklistRun(
            @PathVariable Long runId,
            @RequestParam("managerComments") String managerComments,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        runService.rejectByManager(runId, user, managerComments);
        return "redirect:/dashboard/task-dashboard";
    }

    @GetMapping("/pending")
    public List<ChecklistRun> getPendingChecklists() {
        return runService.getPendingChecklists();
    }
}
