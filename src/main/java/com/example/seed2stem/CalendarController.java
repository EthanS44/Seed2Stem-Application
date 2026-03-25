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

    private final BatchRepository batchRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final BatchActivityLogRepository activityLogRepository;
    private final WorkerSessionRepository workerSessionRepository;

    public CalendarController(BatchRepository batchRepository,
                              ChecklistRunRepository checklistRunRepository,
                              BatchActivityLogRepository activityLogRepository,
                              WorkerSessionRepository workerSessionRepository) {
        this.batchRepository = batchRepository;
        this.checklistRunRepository = checklistRunRepository;
        this.activityLogRepository = activityLogRepository;
        this.workerSessionRepository = workerSessionRepository;
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

        // Batch start and harvest dates
        List<Batch> batches = batchRepository.findByDateRange(startDate, endDate);
        for (Batch b : batches) {
            if (b.getStartDate() != null) {
                events.add(new CalendarEventDTO(
                        "batch-start-" + b.getId(),
                        "Batch " + b.getBatchCode() + " - Started (" + b.getStrain() + ")",
                        b.getStartDate().toString(),
                        null,
                        true,
                        "#2e7d32",
                        "/batches/" + b.getId(),
                        "batch-start"
                ));
            }
            if (b.getHarvestDate() != null) {
                events.add(new CalendarEventDTO(
                        "batch-harvest-" + b.getId(),
                        "Batch " + b.getBatchCode() + " - Harvest",
                        b.getHarvestDate().toString(),
                        null,
                        true,
                        "#e65100",
                        "/batches/" + b.getId(),
                        "batch-harvest"
                ));
            }
        }

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

        // Batch activity logs (status changes)
        List<BatchActivityLog> logs = activityLogRepository.findByTimestampBetween(startDateTime, endDateTime);
        for (BatchActivityLog l : logs) {
            String batchCode = l.getBatch() != null ? l.getBatch().getBatchCode() : "?";
            Long batchId = l.getBatch() != null ? l.getBatch().getId() : 0;
            String from = l.getFromStatus() != null ? l.getFromStatus().name() : "NEW";
            events.add(new CalendarEventDTO(
                    "log-" + l.getId(),
                    batchCode + ": " + from + " → " + l.getToStatus().name(),
                    l.getTimestamp().toString(),
                    null,
                    false,
                    "#8e24aa",
                    "/batches/" + batchId,
                    "status-change"
            ));
        }

        // Worker sessions
        List<WorkerSession> sessions = workerSessionRepository.findBySessionDateBetween(startDate, endDate);
        for (WorkerSession w : sessions) {
            Long batchId = w.getBatch() != null ? w.getBatch().getId() : 0;
            events.add(new CalendarEventDTO(
                    "session-" + w.getId(),
                    w.getSessionType() + " - " + w.getWorkerName(),
                    w.getSessionDate().toString(),
                    null,
                    true,
                    "#00695c",
                    "/batches/" + batchId,
                    "worker-session"
            ));
        }

        return ResponseEntity.ok(events);
    }
}
