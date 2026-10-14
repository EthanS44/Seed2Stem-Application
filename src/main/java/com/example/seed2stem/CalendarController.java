package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CalendarController {

    private final ChecklistRunRepository checklistRunRepository;

    public CalendarController(ChecklistRunRepository checklistRunRepository) {
        this.checklistRunRepository = checklistRunRepository;
    }

    @GetMapping("/calendar")
    public String calendarPage(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        return "calendar";
    }

    @GetMapping("/api/calendar/events")
    @ResponseBody
    public ResponseEntity<List<CalendarEventDTO>> getEvents(
            @RequestParam String start,
            @RequestParam String end,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate startDate = LocalDate.parse(start.substring(0, 10));
        LocalDate endDate = LocalDate.parse(end.substring(0, 10));
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<CalendarEventDTO> events = new ArrayList<>();

        // Checklist runs
        List<ChecklistRun> runs = checklistRunRepository.findByStartTimeBetween(startDateTime, endDateTime);
        for (ChecklistRun r : runs) {
            String userName = r.getCompletedBy() != null ? r.getCompletedBy().getName() : "Unknown";
            events.add(new CalendarEventDTO(
                    "run-" + r.getId(),
                    r.getChecklistName() + " - " + userName,
                    r.getStartTime().toString(),
                    r.getEndTime() != null ? r.getEndTime().toString() : null,
                    false,
                    "#0277bd",
                    "/checklist-runs/runs/" + r.getId(),
                    "checklist-run"
            ));
        }

        return ResponseEntity.ok(events);
    }
}
