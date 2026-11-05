package com.example.seed2stem;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds the Alerts section shown on every dashboard. Alerts are worked out from the
 * current state of things, so each one goes away by itself once it's been dealt with.
 */
@Service
public class NotificationService {

    /** Approvals stay in the list this long, so people see their work went through. */
    static final Duration APPROVED_SHOWN_FOR = Duration.ofHours(24);
    /** User-created tasks can't be resubmitted, so their rejections drop off after this. */
    static final Duration USER_TASK_REJECTION_SHOWN_FOR = Duration.ofDays(7);
    /** Approvals waiting longer than this are flagged as overdue. */
    static final Duration APPROVAL_OVERDUE_AFTER = Duration.ofHours(24);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private final ChecklistRunRepository runRepo;
    private final ChecklistRunService runService;
    private final TimeEntryService timeEntryService;
    private final BatchRecordService batchRecordService;
    private final PayrollService payrollService;
    private final RegistrationRequestRepository registrationRequestRepo;
    private final PasswordResetRequestRepository passwordResetRequestRepo;
    private final TicketRepository ticketRepo;

    public NotificationService(ChecklistRunRepository runRepo,
                               ChecklistRunService runService,
                               TimeEntryService timeEntryService,
                               BatchRecordService batchRecordService,
                               PayrollService payrollService,
                               RegistrationRequestRepository registrationRequestRepo,
                               PasswordResetRequestRepository passwordResetRequestRepo,
                               TicketRepository ticketRepo) {
        this.runRepo = runRepo;
        this.runService = runService;
        this.timeEntryService = timeEntryService;
        this.batchRecordService = batchRecordService;
        this.payrollService = payrollService;
        this.registrationRequestRepo = registrationRequestRepo;
        this.passwordResetRequestRepo = passwordResetRequestRepo;
        this.ticketRepo = ticketRepo;
    }

    @Transactional(readOnly = true)
    public List<Notification> forUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> out = new ArrayList<>();

        addOwnWork(user, now, out);

        AccountType type = user.getAccountType();
        if (type == AccountType.MANAGER || type == AccountType.DEVELOPER) {
            addApprovals(now, out);
            addPayroll(now, out);
            addFilters(out);
        }
        if (type == AccountType.DEVELOPER) {
            addAdminQueue(out);
        }

        out.sort(Comparator.comparing(Notification::getLevel));
        return out;
    }

    /** The user's own tasks and shift. */
    private void addOwnWork(User user, LocalDateTime now, List<Notification> out) {
        for (ChecklistRun run : runRepo.findRejectedByUser(user)) {
            boolean canResubmit = !run.getTask().isUserCreated();
            if (!canResubmit && !within(run.getAuthorizedAt(), now, USER_TASK_REJECTION_SHOWN_FOR)) continue;
            out.add(new Notification(Notification.Level.DANGER, "bi-x-octagon",
                    run.getTask().getTitle() + " was rejected",
                    decision(run, " left no comment."),
                    "/checklist-runs/runs/" + run.getId(),
                    canResubmit ? "Fix and resubmit" : "View"));
        }

        for (ChecklistRun run : runRepo.findRecentSubmittedByUser(user, PageRequest.of(0, 20))) {
            if (run.getStatus() != ChecklistRunStatus.APPROVED) continue;
            if (!within(run.getAuthorizedAt(), now, APPROVED_SHOWN_FOR)) continue;
            out.add(new Notification(Notification.Level.OK, "bi-check2-circle",
                    run.getTask().getTitle() + " was approved",
                    decision(run, ""),
                    "/checklist-runs/runs/" + run.getId(),
                    "View"));
        }

        timeEntryService.getActiveEntry(user)
                .filter(e -> e.getDate() != null && e.getDate().isBefore(now.toLocalDate()))
                .ifPresent(e -> out.add(new Notification(Notification.Level.WARN, "bi-clock-history",
                        "You're still clocked in from " + e.getDate().format(DAY),
                        "If you forgot to clock out, clock out now and tell your manager when you left.",
                        "/time-clock", "Time Clock")));
    }

    private void addApprovals(LocalDateTime now, List<Notification> out) {
        List<ChecklistRun> pending = runService.getPendingChecklists();
        if (pending.isEmpty()) return;
        long overdue = pending.stream()
                .filter(r -> r.getEndTime() != null && !within(r.getEndTime(), now, APPROVAL_OVERDUE_AFTER))
                .count();
        out.add(new Notification(overdue > 0 ? Notification.Level.WARN : Notification.Level.INFO, "bi-clipboard-check",
                plural(pending.size(), "task is", "tasks are") + " waiting for approval",
                overdue > 0 ? plural(overdue, "has", "have") + " been waiting over a day." : "Review them from the Tasks page.",
                "/dashboard/task-dashboard", "Review"));
    }

    private void addPayroll(LocalDateTime now, List<Notification> out) {
        PayPeriod week = payrollService.resolvePeriod("week", now.toLocalDate(), null, null);
        long flagged = payrollService.build(week, now).getFlaggedCount();
        if (flagged == 0) return;
        out.add(new Notification(Notification.Level.WARN, "bi-cash-stack",
                plural(flagged, "shift needs", "shifts need") + " checking before payroll",
                "Missing clock-outs, auto clock-outs or very long shifts this week.",
                "/payroll#to-check", "Payroll"));
    }

    private void addFilters(List<Notification> out) {
        for (FilterStatus f : batchRecordService.filterStatuses().values()) {
            boolean overdue = "OVERDUE".equals(f.getState());
            if (!overdue && !"DUE".equals(f.getState())) continue;
            out.add(new Notification(overdue ? Notification.Level.DANGER : Notification.Level.WARN, "bi-wind",
                    "Room " + f.getRoom().getDisplayName() + " filters are " + (overdue ? "overdue" : "due"),
                    f.getSummary(), "/batch-records", "Batch Records"));
        }
    }

    private void addAdminQueue(List<Notification> out) {
        long registrations = registrationRequestRepo.findByStatus(RegistrationStatus.PENDING).size();
        if (registrations > 0) {
            out.add(new Notification(Notification.Level.INFO, "bi-person-plus",
                    plural(registrations, "registration request", "registration requests") + " to review",
                    null, "/developer/registration-requests", "Review"));
        }
        long resets = passwordResetRequestRepo.countByStatus(PasswordResetStatus.PENDING);
        if (resets > 0) {
            out.add(new Notification(Notification.Level.INFO, "bi-key",
                    plural(resets, "password reset request", "password reset requests") + " to handle",
                    null, "/developer/password-reset-requests", "Review"));
        }
        long tickets = ticketRepo.countByStatus(TicketStatus.OPEN);
        if (tickets > 0) {
            out.add(new Notification(Notification.Level.INFO, "bi-ticket-perforated",
                    plural(tickets, "open ticket", "open tickets"),
                    null, "/developer/tickets", "Tickets"));
        }
    }

    /** "Pat Kim: “comment”" or "Pat Kim left no comment." */
    private static String decision(ChecklistRun run, String noComment) {
        String who = run.getAuthorizedBy() != null ? run.getAuthorizedBy().getName() : "Manager";
        String comment = run.getManagerComments();
        if (comment != null && !comment.isBlank()) return who + ": “" + comment.trim() + "”";
        return noComment.isEmpty() ? "By " + who + "." : who + noComment;
    }

    private static boolean within(LocalDateTime t, LocalDateTime now, Duration window) {
        return t != null && t.isAfter(now.minus(window));
    }

    private static String plural(long n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
