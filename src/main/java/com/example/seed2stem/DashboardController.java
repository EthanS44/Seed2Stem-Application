package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final TaskRepository taskRepo;
    private final ChecklistRunService checklistRunService;
    private final BatchService batchService;
    private final UserRepository userRepo;
    private final TimeEntryService timeEntryService;
    private final TaskPauseRepository taskPauseRepository;

    public DashboardController(TaskRepository taskRepo,
                               ChecklistRunService checklistRunService,
                               BatchService batchService,
                               UserRepository userRepo,
                               TimeEntryService timeEntryService,
                               TaskPauseRepository taskPauseRepository) {
        this.taskRepo = taskRepo;
        this.checklistRunService = checklistRunService;
        this.batchService = batchService;
        this.userRepo = userRepo;
        this.timeEntryService = timeEntryService;
        this.taskPauseRepository = taskPauseRepository;
    }

    @GetMapping("/home-dashboard")
    public String homeDashboard(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() == AccountType.DEVELOPER) {
            return "redirect:/dashboard/developer-dashboard";
        } else if (user.getAccountType() == AccountType.MANAGER) {
            return "redirect:/dashboard/manager-dashboard";
        } else {
            return "redirect:/dashboard/technician-dashboard";
        }
    }

    @GetMapping("/technician-dashboard")
    public String technicianDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // The tech's own count of tasks they're currently working on
        // (in-progress checklist runs). Used for the "Active Tasks" tile.
        int activeTaskCount = checklistRunService.getActiveRunsForUser(user).size();
        model.addAttribute("activeTaskCount", activeTaskCount);
        model.addAttribute("clockedIn", timeEntryService.isClockedIn(user));
        return "technician-dashboard";
    }

    @GetMapping("/manager-dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // "All Active Tasks" = every in-progress checklist run across all techs
        int allActiveTaskCount = checklistRunService.getAllActiveRuns().size();

        model.addAttribute("pendingCount", checklistRunService.getPendingChecklists().size());
        model.addAttribute("allActiveTaskCount", allActiveTaskCount);
        model.addAttribute("clockedInCount", timeEntryService.countClockedIn());
        return "manager-dashboard";
    }

    @GetMapping("/developer-dashboard")
    public String developerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // "All Active Tasks" = every in-progress checklist run across all techs
        int allActiveTaskCount = checklistRunService.getAllActiveRuns().size();

        model.addAttribute("pendingCount", checklistRunService.getPendingChecklists().size());
        model.addAttribute("allActiveTaskCount", allActiveTaskCount);
        model.addAttribute("clockedInCount", timeEntryService.countClockedIn());
        return "developer-dashboard";
    }

    @GetMapping("/task-dashboard")
    public String taskDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // Available Tasks — standard tasks only (not user-created)
        List<Task> availableTasks = taskRepo.findByUserCreatedFalse();
        model.addAttribute("availableTasks", availableTasks);

        // Active Tasks — managers/developers see ALL in-progress runs (with technician);
        // technicians see only their own.
        boolean isManager = user.getAccountType() == AccountType.MANAGER
                || user.getAccountType() == AccountType.DEVELOPER;
        List<ChecklistRun> activeTasks = isManager
                ? checklistRunService.getAllActiveRuns()
                : checklistRunService.getActiveRunsForUser(user);
        model.addAttribute("activeTasks", activeTasks);

        // For the manager/developer view, mark which runs are currently paused
        // so the row can show a "Paused" badge.
        Set<Long> pausedRunIds = isManager
                ? new HashSet<>(taskPauseRepository.findRunIdsWithOpenPause())
                : Set.of();
        model.addAttribute("pausedRunIds", pausedRunIds);

        model.addAttribute("isManager", isManager);
        if (isManager) {
            model.addAttribute("pendingRuns", checklistRunService.getPendingChecklists());
        }

        return "task-dashboard";
    }
}
