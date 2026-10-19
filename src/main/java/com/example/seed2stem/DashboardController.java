package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final TaskRepository taskRepo;
    private final ChecklistRunService checklistRunService;
    private final UserRepository userRepo;
    private final TimeEntryService timeEntryService;
    private final TaskPauseRepository taskPauseRepository;
    private final ChecklistRunRepository runRepo;
    private final BatchRecordService batchRecordService;
    private final RegistrationRequestRepository registrationRequestRepo;
    private final PasswordResetRequestRepository passwordResetRequestRepo;
    private final TicketRepository ticketRepo;

    public DashboardController(TaskRepository taskRepo,
                               ChecklistRunService checklistRunService,
                               UserRepository userRepo,
                               TimeEntryService timeEntryService,
                               TaskPauseRepository taskPauseRepository,
                               ChecklistRunRepository runRepo,
                               BatchRecordService batchRecordService,
                               RegistrationRequestRepository registrationRequestRepo,
                               PasswordResetRequestRepository passwordResetRequestRepo,
                               TicketRepository ticketRepo) {
        this.taskRepo = taskRepo;
        this.checklistRunService = checklistRunService;
        this.userRepo = userRepo;
        this.timeEntryService = timeEntryService;
        this.taskPauseRepository = taskPauseRepository;
        this.runRepo = runRepo;
        this.batchRecordService = batchRecordService;
        this.registrationRequestRepo = registrationRequestRepo;
        this.passwordResetRequestRepo = passwordResetRequestRepo;
        this.ticketRepo = ticketRepo;
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

        List<ChecklistRun> activeRuns = checklistRunService.getActiveRunsForUser(user);
        Optional<TimeEntry> shift = timeEntryService.getActiveEntry(user);

        addGreeting(model, user);
        model.addAttribute("activeRuns", activeRuns);
        model.addAttribute("activeTaskCount", activeRuns.size());
        model.addAttribute("pausedRunIds", new HashSet<>(taskPauseRepository.findRunIdsWithOpenPause()));
        model.addAttribute("awaitingCount", runRepo.countByCompletedByAndStatus(user, ChecklistRunStatus.PENDING));
        model.addAttribute("recentRuns", runRepo.findRecentSubmittedByUser(user, PageRequest.of(0, 5)));
        model.addAttribute("availableTasks", taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc());
        model.addAttribute("clockedIn", shift.isPresent());
        model.addAttribute("shiftStart", shift.map(TimeEntry::getClockInTime).orElse(null));
        model.addAttribute("hoursThisWeek", hoursThisWeek(user, shift.orElse(null)));
        return "technician-dashboard";
    }

    @GetMapping("/manager-dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        addStaffDashboard(model, user);
        return "staff-dashboard";
    }

    @GetMapping("/developer-dashboard")
    public String developerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        addStaffDashboard(model, user);
        model.addAttribute("registrationCount",
                registrationRequestRepo.findByStatus(RegistrationStatus.PENDING).size());
        model.addAttribute("passwordResetCount", passwordResetRequestRepo.countByStatus(PasswordResetStatus.PENDING));
        model.addAttribute("openTicketCount", ticketRepo.countByStatus(TicketStatus.OPEN));
        return "staff-dashboard";
    }

    /** Shared by the manager and developer dashboards: what needs attention right now. */
    private void addStaffDashboard(Model model, User user) {
        // Oldest first, so whatever has waited longest is at the top.
        List<ChecklistRun> pending = checklistRunService.getPendingChecklists().stream()
                .sorted(Comparator.comparing(ChecklistRun::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ChecklistRun> active = checklistRunService.getAllActiveRuns();
        List<TimeEntry> onTheClock = timeEntryService.getAllActiveEntries();
        List<FilterStatus> filterAlerts = batchRecordService.filterStatuses().values().stream()
                .filter(f -> "DUE".equals(f.getState()) || "OVERDUE".equals(f.getState()))
                .toList();

        addGreeting(model, user);
        model.addAttribute("isDeveloper", user.getAccountType() == AccountType.DEVELOPER);
        model.addAttribute("pendingRuns", pending);
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("activeRuns", active);
        model.addAttribute("allActiveTaskCount", active.size());
        model.addAttribute("pausedRunIds", new HashSet<>(taskPauseRepository.findRunIdsWithOpenPause()));
        model.addAttribute("onTheClock", onTheClock);
        model.addAttribute("clockedInCount", (long) onTheClock.size());
        model.addAttribute("filterAlerts", filterAlerts);
    }

    private static void addGreeting(Model model, User user) {
        int hour = LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        model.addAttribute("greeting", part + ", " + user.getFirstName());
        model.addAttribute("today", LocalDate.now());
    }

    /** Completed shifts since Monday plus the open shift so far, to one decimal. */
    private double hoursThisWeek(User user, TimeEntry openShift) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double hours = timeEntryService.getEntriesForDateRange(user, monday, LocalDate.now()).stream()
                .filter(e -> e.getTotalHours() != null)
                .mapToDouble(TimeEntry::getTotalHours)
                .sum();
        if (openShift != null && openShift.getClockInTime() != null) {
            hours += Duration.between(openShift.getClockInTime(), LocalDateTime.now()).toMinutes() / 60.0;
        }
        return Math.round(hours * 10) / 10.0;
    }

    @GetMapping("/task-dashboard")
    public String taskDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        // Available Tasks — standard tasks only (not user-created), and
        // never archived ones (archived tasks can't be started again).
        List<Task> availableTasks = taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc();
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
