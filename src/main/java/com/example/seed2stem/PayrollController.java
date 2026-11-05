package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Payroll: hours per employee by pay period, for managers (and developers). */
@Controller
@RequestMapping("/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollExportService exportService;
    private final TimeEntryService timeEntryService;
    private final TimeEntryRepository timeEntryRepo;
    private final UserRepository userRepo;

    public PayrollController(PayrollService payrollService,
                             PayrollExportService exportService,
                             TimeEntryService timeEntryService,
                             TimeEntryRepository timeEntryRepo,
                             UserRepository userRepo) {
        this.payrollService = payrollService;
        this.exportService = exportService;
        this.timeEntryService = timeEntryService;
        this.timeEntryRepo = timeEntryRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) String period,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                            HttpSession session, Model model) {
        if (!canView(session)) return redirectFor(session);

        PayPeriod p = payrollService.resolvePeriod(period, date, start, end);
        PayrollReport report = payrollService.report(p);
        addPeriodModel(model, p);
        model.addAttribute("report", report);
        // Shifts a manager should check before running payroll, oldest first.
        List<FlaggedShift> flagged = new ArrayList<>();
        for (PayrollReport.EmployeeHours e : report.getEmployees()) {
            for (PayrollReport.Shift s : e.getShifts()) {
                if (s.isFlagged()) flagged.add(new FlaggedShift(e.getUser(), s));
            }
        }
        flagged.sort((a, b) -> a.getShift().getClockIn().compareTo(b.getShift().getClockIn()));
        model.addAttribute("flagged", flagged);
        return "payroll";
    }

    @GetMapping("/employees/{userId}")
    public String employee(@PathVariable Long userId,
                           @RequestParam(required = false) String period,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                           HttpSession session, Model model) {
        if (!canView(session)) return redirectFor(session);

        PayPeriod p = payrollService.resolvePeriod(period, date, start, end);
        PayrollReport.EmployeeHours hours = payrollService.forEmployee(userId, p);
        if (hours == null) return "redirect:/payroll?" + p.getQuery();

        User viewer = (User) session.getAttribute("loggedInUser");
        addPeriodModel(model, p);
        model.addAttribute("emp", hours);
        model.addAttribute("buckets", PayrollService.buckets(p));
        model.addAttribute("role", PayrollExportService.role(hours.getUser()));
        // Nobody edits their own hours.
        model.addAttribute("canEdit", !hours.getUser().getId().equals(viewer.getId()));
        model.addAttribute("breakdownBase", breakdownBase(viewer, hours.getUser()));
        return "payroll-employee";
    }

    @PostMapping("/entries/{entryId}/edit")
    public String editEntry(@PathVariable Long entryId,
                            @RequestParam String clockInTime,
                            @RequestParam(required = false) String clockOutTime,
                            @RequestParam(required = false) String notes,
                            @RequestParam(required = false) String returnQuery,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!canView(session)) return redirectFor(session);
        User viewer = (User) session.getAttribute("loggedInUser");

        TimeEntry entry = timeEntryRepo.findById(entryId).orElse(null);
        if (entry == null) return "redirect:/payroll";
        String back = backTo(entry.getUser().getId(), returnQuery);

        if (entry.getUser().getId().equals(viewer.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can't edit your own hours. Ask another manager.");
            return back;
        }
        try {
            LocalDateTime in = LocalDateTime.parse(clockInTime);
            LocalDateTime out = clockOutTime != null && !clockOutTime.isBlank() ? LocalDateTime.parse(clockOutTime) : null;
            timeEntryService.updateEntry(entryId, in, out, notes);
            redirectAttributes.addFlashAttribute("success", "Shift updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Couldn't update the shift: " + e.getMessage());
        }
        return back;
    }

    @PostMapping("/entries/{entryId}/delete")
    public String deleteEntry(@PathVariable Long entryId,
                              @RequestParam(required = false) String returnQuery,
                              HttpSession session, RedirectAttributes redirectAttributes) {
        if (!canView(session)) return redirectFor(session);
        User viewer = (User) session.getAttribute("loggedInUser");

        TimeEntry entry = timeEntryRepo.findById(entryId).orElse(null);
        if (entry == null) return "redirect:/payroll";
        String back = backTo(entry.getUser().getId(), returnQuery);

        if (entry.getUser().getId().equals(viewer.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can't change your own hours. Ask another manager.");
            return back;
        }
        timeEntryRepo.delete(entry);
        redirectAttributes.addFlashAttribute("success", "Shift deleted.");
        return back;
    }

    @PostMapping("/employees/{userId}/entries")
    public String addEntry(@PathVariable Long userId,
                           @RequestParam String clockInTime,
                           @RequestParam String clockOutTime,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) String returnQuery,
                           HttpSession session, RedirectAttributes redirectAttributes) {
        if (!canView(session)) return redirectFor(session);
        User viewer = (User) session.getAttribute("loggedInUser");

        User employee = userRepo.findById(userId).orElse(null);
        if (employee == null) return "redirect:/payroll";
        String back = backTo(userId, returnQuery);

        if (employee.getId().equals(viewer.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can't add hours for yourself. Ask another manager.");
            return back;
        }
        try {
            String note = notes != null && !notes.isBlank()
                    ? notes.trim() + " (added by " + viewer.getName() + ")"
                    : "Added by " + viewer.getName();
            timeEntryService.addEntry(employee, LocalDateTime.parse(clockInTime), LocalDateTime.parse(clockOutTime), note);
            redirectAttributes.addFlashAttribute("success", "Shift added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Couldn't add the shift: " + e.getMessage());
        }
        return back;
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String period,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                         HttpSession session) {
        if (!canView(session)) return ResponseEntity.status(403).build();

        PayPeriod p = payrollService.resolvePeriod(period, date, start, end);
        byte[] bytes = exportService.export(payrollService.report(p));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Payroll-" + p.getStart() + "-to-" + p.getEnd() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private void addPeriodModel(Model model, PayPeriod p) {
        model.addAttribute("period", p);
        model.addAttribute("prevPeriod", payrollService.previous(p));
        model.addAttribute("nextPeriod", payrollService.next(p));
        model.addAttribute("periodTypes", PayPeriod.Type.values());
        model.addAttribute("overtimeHours", payrollService.getOvertimeHoursPerWeek());
    }

    private static String backTo(Long userId, String returnQuery) {
        return "redirect:/payroll/employees/" + userId
                + (returnQuery != null && returnQuery.matches("[A-Za-z0-9=&\\-]*") ? "?" + returnQuery : "");
    }

    /** Where the existing shift breakdown page lives for this person, if the viewer may open it. */
    private static String breakdownBase(User viewer, User employee) {
        if (employee.getAccountType() == AccountType.TECHNICIAN) {
            return "/technicians/" + employee.getId() + "/time-entries/";
        }
        if (employee.getAccountType() == AccountType.MANAGER && viewer.getAccountType() == AccountType.DEVELOPER) {
            return "/managers/" + employee.getId() + "/time-entries/";
        }
        return null;
    }

    static boolean canView(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && (user.getAccountType() == AccountType.MANAGER
                || user.getAccountType() == AccountType.DEVELOPER);
    }

    private static String redirectFor(HttpSession session) {
        return session.getAttribute("loggedInUser") == null
                ? "redirect:/auth/login" : "redirect:/dashboard/home-dashboard";
    }

    public static class FlaggedShift {
        private final User user;
        private final PayrollReport.Shift shift;

        FlaggedShift(User user, PayrollReport.Shift shift) {
            this.user = user;
            this.shift = shift;
        }

        public User getUser() { return user; }
        public PayrollReport.Shift getShift() { return shift; }
    }
}
