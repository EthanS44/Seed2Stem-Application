package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final TaskRepository taskRepo;
    private final ChecklistRunService checklistRunService;
    private final BatchService batchService;
    private final UserRepository userRepo;
    private final TimeEntryService timeEntryService;

    public DashboardController(TaskRepository taskRepo,
                               ChecklistRunService checklistRunService,
                               BatchService batchService,
                               UserRepository userRepo,
                               TimeEntryService timeEntryService) {
        this.taskRepo = taskRepo;
        this.checklistRunService = checklistRunService;
        this.batchService = batchService;
        this.userRepo = userRepo;
        this.timeEntryService = timeEntryService;
    }

    @GetMapping("/home-dashboard")
    public String homeDashboard(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() == AccountType.MANAGER) {
            return "redirect:/dashboard/manager-dashboard";
        } else {
            return "redirect:/dashboard/technician-dashboard";
        }
    }

    @GetMapping("/technician-dashboard")
    public String technicianDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        model.addAttribute("taskCount", taskRepo.count());
        model.addAttribute("batchCount", batchService.countActive());
        model.addAttribute("clockedIn", timeEntryService.isClockedIn(user));
        return "technician-dashboard";
    }

    @GetMapping("/manager-dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        model.addAttribute("pendingCount", checklistRunService.getPendingChecklists().size());
        model.addAttribute("batchCount", batchService.countActive());
        model.addAttribute("taskCount", taskRepo.count());
        model.addAttribute("technicianCount", userRepo.countByAccountType(AccountType.TECHNICIAN));
        model.addAttribute("clockedInCount", timeEntryService.countClockedIn());
        return "manager-dashboard";
    }

    @GetMapping("/task-dashboard")
    public String taskDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // Available Tasks — static list, same for all roles
        List<Task> availableTasks = taskRepo.findAll();
        model.addAttribute("availableTasks", availableTasks);

        // Active Tasks — user's own IN_PROGRESS checklist runs
        List<ChecklistRun> activeTasks = checklistRunService.getActiveRunsForUser(user);
        model.addAttribute("activeTasks", activeTasks);

        // Pending Approval — manager only
        boolean isManager = user.getAccountType() == AccountType.MANAGER;
        model.addAttribute("isManager", isManager);
        if (isManager) {
            model.addAttribute("pendingRuns", checklistRunService.getPendingChecklists());
        }

        return "task-dashboard";
    }
}
