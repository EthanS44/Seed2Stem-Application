package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/time-clock")
public class TimeClockController {

    private final TimeEntryService timeEntryService;
    private final ChecklistRunService checklistRunService;

    public TimeClockController(TimeEntryService timeEntryService, ChecklistRunService checklistRunService) {
        this.timeEntryService = timeEntryService;
        this.checklistRunService = checklistRunService;
    }

    @GetMapping
    public String timeClockPage(@RequestParam(required = false) String next,
                                HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Optional<TimeEntry> activeEntry = timeEntryService.getActiveEntry(user);
        model.addAttribute("isClockedIn", activeEntry.isPresent());
        activeEntry.ifPresent(e -> model.addAttribute("activeEntry", e));
        // Open tasks block clocking out, so list them on the page.
        model.addAttribute("openRuns", checklistRunService.getActiveRunsForUser(user));
        // Set when a task page sent the user here to clock in first.
        if (isSafeRedirect(next)) model.addAttribute("next", next);

        model.addAttribute("todayEntries", timeEntryService.getTodayEntries(user));

        LocalDate weekAgo = LocalDate.now().minusDays(7);
        model.addAttribute("recentEntries",
                timeEntryService.getEntriesForDateRange(user, weekAgo, LocalDate.now()));

        return "time-clock";
    }

    @PostMapping("/clock-in")
    public String clockIn(@RequestParam(required = false) String next,
                          HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        try {
            timeEntryService.clockIn(user);
            redirectAttributes.addFlashAttribute("success", "Clocked in successfully!");
            if (isSafeRedirect(next)) return "redirect:" + next;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-clock";
    }

    @PostMapping("/clock-out")
    public String clockOut(@RequestParam(required = false) String notes,
                           HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        try {
            TimeEntry entry = timeEntryService.clockOut(user, notes);
            redirectAttributes.addFlashAttribute("success",
                    "Clocked out! Total: " + entry.getTotalHours() + " hours.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-clock";
    }

    @GetMapping("/history")
    public String timeHistory(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<TimeEntry> entries = timeEntryService.getEntriesForDateRange(user, start, end);
        double totalHours = entries.stream()
                .filter(e -> e.getTotalHours() != null)
                .mapToDouble(TimeEntry::getTotalHours)
                .sum();

        model.addAttribute("entries", entries);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("totalHours", Math.round(totalHours * 100.0) / 100.0);

        return "time-clock-history";
    }

    /** Only same-site paths, so the clock-in form can't be used as an open redirect. */
    static boolean isSafeRedirect(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\");
    }
}
