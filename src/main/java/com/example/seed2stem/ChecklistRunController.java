package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/checklist-runs")
public class ChecklistRunController {

    private final ChecklistRunService runService;
    private final TaskRepository taskRepo;
    private final ChecklistItemRepository itemRepo;

    public ChecklistRunController(ChecklistRunService runService,
                                  TaskRepository taskRepo,
                                  ChecklistItemRepository itemRepo) {
        this.runService = runService;
        this.taskRepo = taskRepo;
        this.itemRepo = itemRepo;
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
        ChecklistRun run = runService.getChecklistById(runId);

        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(
                task.getChecklist().getId());

        List<ChecklistResponse> responses = new ArrayList<>();

        for (ChecklistItem item : items) {
            if (item.getItemType() != ChecklistItemType.QUESTION) continue;
            if (item.getResponseType() == ChecklistResponseType.NONE) continue;

            ChecklistResponse resp = new ChecklistResponse();
            resp.setChecklistItem(item);
            resp.setChecklistRun(run);

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
                default -> { /* NONE items are skipped above */ }
            }

            responses.add(resp);
        }

        run.setEndTime(LocalDateTime.now());
        run.setResponses(responses);
        runService.submitByTechnician(run);

        return "redirect:/dashboard/task-dashboard";
    }

    @PostMapping("/pause")
    public String pauseChecklist(@RequestParam Long taskId,
                                 @RequestParam Long runId,
                                 @RequestParam MultiValueMap<String, String> params,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Task task = taskRepo.findById(taskId).orElseThrow();
        ChecklistRun run = runService.getChecklistById(runId);

        List<ChecklistItem> items = itemRepo.findByChecklistIdOrderByDisplayOrder(
                task.getChecklist().getId());

        List<ChecklistResponse> responses = new ArrayList<>();

        for (ChecklistItem item : items) {
            if (item.getItemType() != ChecklistItemType.QUESTION) continue;
            if (item.getResponseType() == ChecklistResponseType.NONE) continue;

            ChecklistResponse resp = new ChecklistResponse();
            resp.setChecklistItem(item);
            resp.setChecklistRun(run);

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
                default -> { /* NONE items skipped above */ }
            }

            responses.add(resp);
        }

        runService.pauseByUser(runId, responses);
        return "redirect:/dashboard/task-dashboard";
    }

    @GetMapping("/runs/{runId}")
    public String reviewChecklistRun(@PathVariable Long runId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        ChecklistRun run = runService.getChecklistById(runId);
        if (run == null) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<ChecklistItem> items = run.getTask().getChecklist().getItems();
        List<ChecklistResponse> responses = run.getResponses();

        Map<Long, ChecklistResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(r -> r.getChecklistItem().getId(), r -> r));

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
