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
import java.util.List;

@Controller
@RequestMapping("/technicians")
public class TechnicianProfileController {

    private final UserRepository userRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final TimeEntryService timeEntryService;

    public TechnicianProfileController(UserRepository userRepository,
                                       ChecklistRunRepository checklistRunRepository,
                                       TimeEntryService timeEntryService) {
        this.userRepository = userRepository;
        this.checklistRunRepository = checklistRunRepository;
        this.timeEntryService = timeEntryService;
    }

    @GetMapping
    public String listTechnicians(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/dashboard/home-dashboard";

        List<User> technicians = userRepository.findByAccountType(AccountType.TECHNICIAN);
        model.addAttribute("technicians", technicians);
        return "technician-list";
    }

    @GetMapping("/{id}")
    public String technicianProfile(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/dashboard/home-dashboard";

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
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/dashboard/home-dashboard";

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
}
