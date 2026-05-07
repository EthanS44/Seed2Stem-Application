package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/technicians")
public class TechnicianProfileController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final UserRepository userRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final TimeEntryService timeEntryService;
    private final TimeEntryRepository timeEntryRepository;
    private final TaskPauseRepository taskPauseRepository;

    public TechnicianProfileController(UserRepository userRepository,
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
    public String listTechnicians(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER
                && user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<User> technicians = userRepository.findByAccountType(AccountType.TECHNICIAN);
        model.addAttribute("technicians", technicians);
        return "technician-list";
    }

    @GetMapping("/{id}")
    public String technicianProfile(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER
                && user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        User technician = userRepository.findById(id).orElse(null);
        if (technician == null) return "redirect:/technicians";

        List<ChecklistRun> completedRuns = checklistRunRepository.findCompletedByUserOrderByStartTimeDesc(technician);

        // Time entries — last 30 days
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        List<TimeEntry> timeEntries = timeEntryService.getEntriesForDateRange(technician, start, end);
        boolean isClockedIn = timeEntryService.isClockedIn(technician);
        double totalHoursMonth = timeEntries.stream()
                .filter(e -> e.getTotalHours() != null)
                .mapToDouble(TimeEntry::getTotalHours)
                .sum();

        model.addAttribute("technician", technician);
        model.addAttribute("completedRuns", completedRuns);
        model.addAttribute("timeEntries", timeEntries);
        model.addAttribute("techClockedIn", isClockedIn);
        model.addAttribute("totalHoursMonth", Math.round(totalHoursMonth * 100.0) / 100.0);
        return "technician-profile";
    }

    @PostMapping("/{techId}/time-entries/{entryId}/edit")
    public String editTimeEntry(@PathVariable Long techId,
                                @PathVariable Long entryId,
                                @RequestParam String clockInTime,
                                @RequestParam(required = false) String clockOutTime,
                                @RequestParam(required = false) String notes,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER
                && user.getAccountType() != AccountType.DEVELOPER) {
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
        return "redirect:/technicians/" + techId;
    }

    @GetMapping("/{techId}/time-entries/{entryId}")
    public String timeEntryDetail(@PathVariable Long techId,
                                  @PathVariable Long entryId,
                                  HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER
                && user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        User technician = userRepository.findById(techId).orElse(null);
        if (technician == null) return "redirect:/technicians";

        TimeEntry entry = timeEntryRepository.findById(entryId).orElse(null);
        if (entry == null) return "redirect:/technicians/" + techId;

        LocalDateTime shiftStart = entry.getClockInTime();
        boolean isActive = entry.getClockOutTime() == null;
        LocalDateTime shiftEnd = isActive ? LocalDateTime.now() : entry.getClockOutTime();
        long totalMinutes = ChronoUnit.MINUTES.between(shiftStart, shiftEnd);
        if (totalMinutes <= 0) totalMinutes = 1;

        List<ChecklistRun> runs = checklistRunRepository
                .findByUserAndStartTimeBetween(technician, shiftStart, shiftEnd);

        List<TimelineSegment> segments = new ArrayList<>();
        LocalDateTime cursor = shiftStart;

        for (ChecklistRun run : runs) {
            if (run.getStartTime() == null) continue;
            long gap = ChronoUnit.MINUTES.between(cursor, run.getStartTime());
            if (gap > 0) {
                segments.add(new TimelineSegment(
                        "downtime", "Downtime", gap, totalMinutes,
                        cursor.format(TIME_FMT), run.getStartTime().format(TIME_FMT), null));
            }
            LocalDateTime runEnd = run.getEndTime() != null ? run.getEndTime() : run.getStartTime();
            String taskLabel = run.getTask() != null ? run.getTask().getTitle() : run.getChecklistName();

            // Pauses within this run carve the task block into [task][pause][task]... slices.
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
                    segments.add(new TimelineSegment(
                            "task", taskLabel, taskMinutes, totalMinutes,
                            inner.format(TIME_FMT), pStart.format(TIME_FMT), run.getId()));
                }
                long pauseMinutes = ChronoUnit.MINUTES.between(pStart, pEnd);
                if (pauseMinutes < 1) pauseMinutes = 1;
                String pauseLabel = p.getReason() != null && !p.getReason().isBlank()
                        ? "Pause: " + p.getReason() : "Pause";
                segments.add(new TimelineSegment(
                        "pause", pauseLabel, pauseMinutes, totalMinutes,
                        pStart.format(TIME_FMT), pEnd.format(TIME_FMT), run.getId()));
                inner = pEnd;
            }
            // Trailing task slice after the last pause (or whole run if no pauses)
            long trailing = ChronoUnit.MINUTES.between(inner, runEnd);
            if (trailing < 1 && segments.stream().noneMatch(s ->
                    s.runId != null && s.runId.equals(run.getId()))) {
                // Tiny run with no pauses — still show at least 1 min
                trailing = 1;
            }
            if (trailing >= 1) {
                segments.add(new TimelineSegment(
                        "task", taskLabel, trailing, totalMinutes,
                        inner.format(TIME_FMT), runEnd.format(TIME_FMT), run.getId()));
            }

            cursor = runEnd;
        }

        long remainingGap = ChronoUnit.MINUTES.between(cursor, shiftEnd);
        if (remainingGap > 0) {
            segments.add(new TimelineSegment(
                    "downtime", "Downtime", remainingGap, totalMinutes,
                    cursor.format(TIME_FMT), shiftEnd.format(TIME_FMT), null));
        }

        long taskMinutesTotal = segments.stream()
                .filter(s -> "task".equals(s.type)).mapToLong(s -> s.durationMinutes).sum();
        long pauseMinutesTotal = segments.stream()
                .filter(s -> "pause".equals(s.type)).mapToLong(s -> s.durationMinutes).sum();
        long downtimeMinutesTotal = totalMinutes - taskMinutesTotal - pauseMinutesTotal;
        if (downtimeMinutesTotal < 0) downtimeMinutesTotal = 0;

        model.addAttribute("technician", technician);
        model.addAttribute("entry", entry);
        model.addAttribute("segments", segments);
        model.addAttribute("totalMinutes", totalMinutes);
        model.addAttribute("taskMinutesTotal", taskMinutesTotal);
        model.addAttribute("pauseMinutesTotal", pauseMinutesTotal);
        model.addAttribute("downtimeMinutesTotal", downtimeMinutesTotal);
        model.addAttribute("isActive", isActive);
        return "time-entry-detail";
    }

    public static class TimelineSegment {
        public final String type;
        public final String label;
        public final double percentage;
        public final String startFormatted;
        public final String endFormatted;
        public final long durationMinutes;
        public final Long runId;

        TimelineSegment(String type, String label, long durationMinutes, long totalMinutes,
                        String startFormatted, String endFormatted, Long runId) {
            this.type = type;
            this.label = label;
            this.durationMinutes = durationMinutes;
            this.percentage = (durationMinutes / (double) totalMinutes) * 100.0;
            this.startFormatted = startFormatted;
            this.endFormatted = endFormatted;
            this.runId = runId;
        }
    }
}
