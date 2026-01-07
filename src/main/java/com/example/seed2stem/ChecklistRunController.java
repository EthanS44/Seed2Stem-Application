package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Technician submits checklist
    @PostMapping("/submit")
    public String submitChecklist(@RequestParam Long taskId, @RequestParam Long runId, @RequestParam MultiValueMap<String, String> params, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Task task = taskRepo.findById(taskId).orElseThrow();

        ChecklistRun run = runService.getChecklistById(runId);
        run.setEndTime(LocalDateTime.now());

        List<ChecklistResponse> responses = new ArrayList<>();

        for (String key : params.keySet()) {

            if (key.startsWith("bool_")) {
                Long itemId = Long.valueOf(key.substring(5));
                ChecklistItem item = itemRepo.findById(itemId).orElseThrow();

                ChecklistResponse r = new ChecklistResponse();
                r.setChecklistItem(item);
                r.setBooleanAnswer(Boolean.valueOf(params.getFirst(key)));
                r.setTextAnswer(params.getFirst("text_" + itemId));
                r.setChecklistRun(run);

                responses.add(r);
            }

            if (key.startsWith("num_")) {
                Long itemId = Long.valueOf(key.substring(4));
                ChecklistItem item = itemRepo.findById(itemId).orElseThrow();

                ChecklistResponse r = new ChecklistResponse();
                r.setChecklistItem(item);
                r.setNumericAnswer(Double.valueOf(params.getFirst(key)));
                r.setChecklistRun(run);

                responses.add(r);
            }
        }

        run.setResponses(responses);
        runService.submitByTechnician(run);

        return "redirect:/dashboard/task-dashboard";
    }

    @GetMapping("/runs/{runId}")
    public String reviewChecklistRun(@PathVariable Long runId, Model model) {
        ChecklistRun run = runService.getChecklistById(runId);

        model.addAttribute("run", run);
        model.addAttribute("task", run.getTask());
        model.addAttribute("responses", run.getResponses());

        return "checklist-review-view";
    }

    // Manager approves checklist
    @PostMapping("/runs/{runId}/approve")
    public String approveChecklistRun(
            @PathVariable Long runId,
            @RequestParam("managerComments") String managerComments,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedInUser");
        runService.authorizeByManager(runId, user, managerComments);

        // Redirect back to the manager dashboard
        return "redirect:/dashboard/task-dashboard";
    }

    // Get all pending checklists (for manager dashboard)
    @GetMapping("/pending")
    public List<ChecklistRun> getPendingChecklists() {
        return runService.getPendingChecklists();
    }
}


