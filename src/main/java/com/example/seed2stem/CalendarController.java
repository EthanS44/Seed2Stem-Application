package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class CalendarController {

    private final ChecklistRunRepository checklistRunRepository;
    private final UserRepository userRepository;

    public CalendarController(ChecklistRunRepository checklistRunRepository,
                              UserRepository userRepository) {
        this.checklistRunRepository = checklistRunRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/calendar")
    public String calendarPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        boolean staff = isStaff(user);
        model.addAttribute("isStaff", staff);
        if (staff) {
            // Everyone who can do tasks, for the person filter.
            List<User> people = new ArrayList<>(userRepository.findByAccountType(AccountType.TECHNICIAN));
            people.addAll(userRepository.findByAccountType(AccountType.MANAGER));
            people.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
            model.addAttribute("people", people);
        }
        return "calendar";
    }

    /**
     * Task runs between {@code start} and {@code end}. Technicians only get their
     * own runs; managers and developers get everyone's, optionally for one person.
     */
    @GetMapping("/api/calendar/events")
    @ResponseBody
    public ResponseEntity<List<CalendarEventDTO>> getEvents(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Long userId,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate startDate = LocalDate.parse(start.substring(0, 10));
        LocalDate endDate = LocalDate.parse(end.substring(0, 10));
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<ChecklistRun> runs = isStaff(user)
                ? checklistRunRepository.findByStartTimeBetween(startDateTime, endDateTime)
                : checklistRunRepository.findByUserAndStartTimeBetween(user, startDateTime, endDateTime);

        List<CalendarEventDTO> events = new ArrayList<>();
        for (ChecklistRun r : runs) {
            if (r.getStartTime() == null) continue;
            User who = r.getCompletedBy();
            if (userId != null && (who == null || !userId.equals(who.getId()))) continue;

            ChecklistRunStatus status = r.getStatus() != null ? r.getStatus() : ChecklistRunStatus.PENDING;
            String title = r.getTask() != null ? r.getTask().getTitle() : r.getChecklistName();
            CalendarEventDTO e = new CalendarEventDTO(
                    "run-" + r.getId(),
                    title,
                    r.getStartTime().toString(),
                    r.getEndTime() != null ? r.getEndTime().toString() : null,
                    false,
                    colorFor(status),
                    "/checklist-runs/runs/" + r.getId(),
                    "checklist-run");
            e.setStatus(status.name());
            e.setStatusLabel(labelFor(status));
            e.setPerson(who != null ? who.getName() : "Unknown");
            e.setPersonId(who != null ? who.getId() : null);
            events.add(e);
        }

        return ResponseEntity.ok(events);
    }

    private static boolean isStaff(User user) {
        return user.getAccountType() == AccountType.MANAGER || user.getAccountType() == AccountType.DEVELOPER;
    }

    /** Matches the status pills used elsewhere in the app. */
    static String colorFor(ChecklistRunStatus status) {
        return switch (status) {
            case APPROVED -> "#2e7d32";
            case PENDING -> "#d98a00";
            case REJECTED -> "#c62828";
            case IN_PROGRESS -> "#1565c0";
        };
    }

    static String labelFor(ChecklistRunStatus status) {
        return switch (status) {
            case APPROVED -> "Approved";
            case PENDING -> "Awaiting approval";
            case REJECTED -> "Rejected";
            case IN_PROGRESS -> "In progress";
        };
    }
}
