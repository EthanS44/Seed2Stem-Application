package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/managers")
public class ManagerProfileController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final UserRepository userRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final TimeEntryService timeEntryService;
    private final TimeEntryRepository timeEntryRepository;
    private final TaskPauseRepository taskPauseRepository;

    public ManagerProfileController(UserRepository userRepository,
                                    ChecklistRunRepository checklistRunRepository,
                                    TimeEntryService timeEntryService,
                                    TimeEntryRepository timeEntryRepository,
                                    TaskPauseRepository taskPauseRepository) {
        this.userRepository = userRepository;
        this.checklistRunRepository = checklistRunRepository;
        this.timeEntryService = timeEntryService;
        this.timeEntryRepository = timeEntryRepository;
        this.taskPauseRepository = taskPauseRepository;
    }

    @GetMapping
    public String listManagers(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<User> managers = userRepository.findByAccountType(AccountType.MANAGER);

        Map<Long, TimeEntry> activeEntryByUserId = new HashMap<>();
        for (TimeEntry entry : timeEntryService.getAllActiveEntries()) {
            if (entry.getUser() != null) {
                activeEntryByUserId.put(entry.getUser().getId(), entry);
            }
        }
        Map<Long, ChecklistRun> activeRunByUserId = new HashMap<>();
        List<ChecklistRun> allActiveRuns = checklistRunRepository
                .findAllByStatusWithUserAndTask(ChecklistRunStatus.IN_PROGRESS);
        for (ChecklistRun run : allActiveRuns) {
            if (run.getCompletedBy() == null) continue;
            Long uid = run.getCompletedBy().getId();
            ChecklistRun existing = activeRunByUserId.get(uid);
            if (existing == null
                    || (run.getStartTime() != null && existing.getStartTime() != null
                        && run.getStartTime().isBefore(existing.getStartTime()))) {
                activeRunByUserId.put(uid, run);
            }
        }
        Set<Long> runsWithOpenPause = new HashSet<>(taskPauseRepository.findRunIdsWithOpenPause());

        List<ManagerSummary> summaries = new ArrayList<>();
        for (User mgr : managers) {
            TimeEntry activeEntry = activeEntryByUserId.get(mgr.getId());
            ChecklistRun activeRun = activeRunByUserId.get(mgr.getId());
            boolean paused = activeRun != null && runsWithOpenPause.contains(activeRun.getId());
            String taskLabel = null;
            Long runId = null;
            LocalDateTime taskStartedAt = null;
            if (activeRun != null) {
                taskLabel = activeRun.getTask() != null
                        ? activeRun.getTask().getTitle()
                        : activeRun.getChecklistName();
                runId = activeRun.getId();
                taskStartedAt = activeRun.getStartTime();
            }
            summaries.add(new ManagerSummary(
                    mgr,
                    activeEntry != null,
                    activeEntry != null ? activeEntry.getClockInTime() : null,
                    paused,
                    taskLabel,
                    runId,
                    taskStartedAt));
        }

        summaries.sort(Comparator
                .comparingInt((ManagerSummary s) -> s.clockedIn ? 0 : 1)
                .thenComparingInt(s -> s.currentTaskLabel != null ? 0 : 1)
                .thenComparing(s -> s.manager.getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        model.addAttribute("summaries", summaries);
        model.addAttribute("clockedInCount", summaries.stream().filter(s -> s.clockedIn).count());
        model.addAttribute("workingCount", summaries.stream().filter(s -> s.currentTaskLabel != null && !s.paused).count());
        model.addAttribute("pausedCount", summaries.stream().filter(s -> s.paused).count());
        return "manager-list";
    }

    @GetMapping("/{id}")
    public String managerProfile(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        User manager = userRepository.findById(id).orElse(null);
        if (manager == null) return "redirect:/managers";

        List<ChecklistRun> completedRuns = checklistRunRepository.findCompletedByUserOrderByStartTimeDesc(manager);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        List<TimeEntry> timeEntries = timeEntryService.getEntriesForDateRange(manager, start, end);
        boolean isClockedIn = timeEntryService.isClockedIn(manager);
        double totalHoursMonth = timeEntries.stream()
                .filter(e -> e.getTotalHours() != null)
                .mapToDouble(TimeEntry::getTotalHours)
                .sum();

        model.addAttribute("manager", manager);
        model.addAttribute("completedRuns", completedRuns);
        model.addAttribute("timeEntries", timeEntries);
        model.addAttribute("mgrClockedIn", isClockedIn);
        model.addAttribute("totalHoursMonth", Math.round(totalHoursMonth * 100.0) / 100.0);
        return "manager-profile";
    }

    @PostMapping("/{mgrId}/time-entries/{entryId}/edit")
    public String editTimeEntry(@PathVariable Long mgrId,
                                @PathVariable Long entryId,
                                @RequestParam String clockInTime,
                                @RequestParam(required = false) String clockOutTime,
                                @RequestParam(required = false) String notes,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        try {
            LocalDateTime parsedIn = LocalDateTime.parse(clockInTime);
            LocalDateTime parsedOut = (clockOutTime != null && !clockOutTime.isBlank())
                    ? LocalDateTime.parse(clockOutTime) : null;
            timeEntryService.updateEntry(entryId, parsedIn, parsedOut, notes);
            redirectAttributes.addFlashAttribute("success", "Time entry updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/managers/" + mgrId;
    }

    @GetMapping("/{mgrId}/time-entries/{entryId}")
    public String timeEntryDetail(@PathVariable Long mgrId,
                                  @PathVariable Long entryId,
                                  HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        User manager = userRepository.findById(mgrId).orElse(null);
        if (manager == null) return "redirect:/managers";

        TimeEntry entry = timeEntryRepository.findById(entryId).orElse(null);
        if (entry == null) return "redirect:/managers/" + mgrId;

        LocalDateTime shiftStart = entry.getClockInTime();
        boolean isActive = entry.getClockOutTime() == null;
        LocalDateTime shiftEnd = isActive ? LocalDateTime.now() : entry.getClockOutTime();
        long totalMinutes = ChronoUnit.MINUTES.between(shiftStart, shiftEnd);
        if (totalMinutes <= 0) totalMinutes = 1;

        List<ChecklistRun> runs = checklistRunRepository
                .findByUserAndStartTimeBetween(manager, shiftStart, shiftEnd);

        List<TechnicianProfileController.TimelineSegment> segments = new ArrayList<>();
        LocalDateTime cursor = shiftStart;

        for (ChecklistRun run : runs) {
            if (run.getStartTime() == null) continue;
            long gap = ChronoUnit.MINUTES.between(cursor, run.getStartTime());
            if (gap > 0) {
                segments.add(new TechnicianProfileController.TimelineSegment(
                        "downtime", "Downtime", gap, totalMinutes,
                        cursor.format(TIME_FMT), run.getStartTime().format(TIME_FMT), null));
            }
            LocalDateTime runEnd = run.getEndTime() != null ? run.getEndTime() : run.getStartTime();
            String taskLabel = run.getTask() != null ? run.getTask().getTitle() : run.getChecklistName();

            List<TaskPause> pauses = taskPauseRepository.findByChecklistRunOrderByStartTimeAsc(run);
            LocalDateTime inner = run.getStartTime();
            for (TaskPause p : pauses) {
                if (p.getStartTime() == null) continue;
                LocalDateTime pStart = p.getStartTime();
                LocalDateTime pEnd = p.getEndTime() != null ? p.getEndTime() : runEnd;
                if (pStart.isBefore(inner)) pStart = inner;
                if (pEnd.isAfter(runEnd)) pEnd = runEnd;
                if (!pStart.isBefore(pEnd)) continue;

                long taskMinutes = ChronoUnit.MINUTES.between(inner, pStart);
                if (taskMinutes >= 1) {
                    segments.add(new TechnicianProfileController.TimelineSegment(
                            "task", taskLabel, taskMinutes, totalMinutes,
                            inner.format(TIME_FMT), pStart.format(TIME_FMT), run.getId()));
                }
                long pauseMinutes = ChronoUnit.MINUTES.between(pStart, pEnd);
                if (pauseMinutes < 1) pauseMinutes = 1;
                String pauseLabel = p.getReason() != null && !p.getReason().isBlank()
                        ? "Pause: " + p.getReason() : "Pause";
                segments.add(new TechnicianProfileController.TimelineSegment(
                        "pause", pauseLabel, pauseMinutes, totalMinutes,
                        pStart.format(TIME_FMT), pEnd.format(TIME_FMT), run.getId()));
                inner = pEnd;
            }
            long trailing = ChronoUnit.MINUTES.between(inner, runEnd);
            if (trailing < 1 && segments.stream().noneMatch(s ->
                    s.runId != null && s.runId.equals(run.getId()))) {
                trailing = 1;
            }
            if (trailing >= 1) {
                segments.add(new TechnicianProfileController.TimelineSegment(
                        "task", taskLabel, trailing, totalMinutes,
                        inner.format(TIME_FMT), runEnd.format(TIME_FMT), run.getId()));
            }

            cursor = runEnd;
        }

        long remainingGap = ChronoUnit.MINUTES.between(cursor, shiftEnd);
        if (remainingGap > 0) {
            segments.add(new TechnicianProfileController.TimelineSegment(
                    "downtime", "Downtime", remainingGap, totalMinutes,
                    cursor.format(TIME_FMT), shiftEnd.format(TIME_FMT), null));
        }

        long taskMinutesTotal = segments.stream()
                .filter(s -> "task".equals(s.type)).mapToLong(s -> s.durationMinutes).sum();
        long pauseMinutesTotal = segments.stream()
                .filter(s -> "pause".equals(s.type)).mapToLong(s -> s.durationMinutes).sum();
        long downtimeMinutesTotal = totalMinutes - taskMinutesTotal - pauseMinutesTotal;
        if (downtimeMinutesTotal < 0) downtimeMinutesTotal = 0;

        // time-entry-detail.html reads ${technician} for the name header; we reuse it.
        model.addAttribute("technician", manager);
        model.addAttribute("entry", entry);
        model.addAttribute("segments", segments);
        model.addAttribute("totalMinutes", totalMinutes);
        model.addAttribute("taskMinutesTotal", taskMinutesTotal);
        model.addAttribute("pauseMinutesTotal", pauseMinutesTotal);
        model.addAttribute("downtimeMinutesTotal", downtimeMinutesTotal);
        model.addAttribute("isActive", isActive);
        model.addAttribute("backUrl", "/managers/" + mgrId);
        return "time-entry-detail";
    }

    public static class ManagerSummary {
        public final User manager;
        public final boolean clockedIn;
        public final LocalDateTime clockInTime;
        public final boolean paused;
        public final String currentTaskLabel;
        public final Long currentRunId;
        public final LocalDateTime taskStartedAt;

        ManagerSummary(User manager, boolean clockedIn, LocalDateTime clockInTime,
                       boolean paused, String currentTaskLabel, Long currentRunId,
                       LocalDateTime taskStartedAt) {
            this.manager = manager;
            this.clockedIn = clockedIn;
            this.clockInTime = clockInTime;
            this.paused = paused;
            this.currentTaskLabel = currentTaskLabel;
            this.currentRunId = currentRunId;
            this.taskStartedAt = taskStartedAt;
        }

        public User getManager() { return manager; }
        public boolean isClockedIn() { return clockedIn; }
        public LocalDateTime getClockInTime() { return clockInTime; }
        public boolean isPaused() { return paused; }
        public String getCurrentTaskLabel() { return currentTaskLabel; }
        public Long getCurrentRunId() { return currentRunId; }
        public LocalDateTime getTaskStartedAt() { return taskStartedAt; }
    }
}
