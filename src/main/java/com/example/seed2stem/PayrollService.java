package com.example.seed2stem;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Works out paid hours per employee for a pay period.
 *
 * <p>Hours come straight from clock-in/clock-out times. Overtime is anything over
 * {@code payroll.overtime-hours-per-week} (default 40) within a Monday–Sunday week,
 * counted in clock-in order, so a period that starts mid-week still sees the hours
 * worked earlier that week.
 */
@Service
public class PayrollService {

    /** Shifts longer than this get flagged for a second look. */
    static final long LONG_SHIFT_MINUTES = 12 * 60;
    /** Short periods get a column per day; longer ones a column per week. */
    static final int DAILY_COLUMNS_UP_TO = 16;

    private final TimeEntryRepository timeEntryRepo;
    private final UserRepository userRepo;
    private final LocalDate biweeklyAnchor;
    private final long overtimeMinutesPerWeek;

    public PayrollService(TimeEntryRepository timeEntryRepo,
                          UserRepository userRepo,
                          @Value("${payroll.biweekly-anchor:2026-01-05}") String biweeklyAnchor,
                          @Value("${payroll.overtime-hours-per-week:40}") int overtimeHoursPerWeek) {
        this.timeEntryRepo = timeEntryRepo;
        this.userRepo = userRepo;
        this.biweeklyAnchor = LocalDate.parse(biweeklyAnchor);
        this.overtimeMinutesPerWeek = overtimeHoursPerWeek * 60L;
    }

    /** The period to show for the page's query parameters; defaults to the current week. */
    public PayPeriod resolvePeriod(String type, LocalDate date, LocalDate start, LocalDate end) {
        PayPeriod.Type t = PayPeriod.Type.fromParam(type);
        if (t == PayPeriod.Type.CUSTOM) {
            LocalDate s = start != null ? start : LocalDate.now().minusDays(13);
            LocalDate e = end != null ? end : LocalDate.now();
            return PayPeriod.custom(s, e);
        }
        return PayPeriod.containing(t, date != null ? date : LocalDate.now(), biweeklyAnchor);
    }

    public PayPeriod previous(PayPeriod p) { return p.previous(biweeklyAnchor); }
    public PayPeriod next(PayPeriod p) { return p.next(biweeklyAnchor); }
    public long getOvertimeHoursPerWeek() { return overtimeMinutesPerWeek / 60; }

    @Transactional(readOnly = true)
    public PayrollReport report(PayPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        PayrollReport report = build(period, now);
        // Compare like with like: while a period is still running, only count the
        // previous period up to the same day.
        PayPeriod prev = previous(period);
        LocalDate cutoff = period.contains(now.toLocalDate())
                ? prev.getStart().plusDays(java.time.temporal.ChronoUnit.DAYS.between(period.getStart(), now.toLocalDate()))
                : prev.getEnd();
        long prevMinutes = build(prev, now).getEmployees().stream()
                .flatMap(e -> e.getShifts().stream())
                .filter(s -> !s.getDate().isAfter(cutoff))
                .mapToLong(PayrollReport.Shift::getMinutes)
                .sum();
        report.setPreviousTotalMinutes(prevMinutes);
        report.setComparedToPartial(period.contains(now.toLocalDate()));
        return report;
    }

    /** One employee's hours for the period (null if the user doesn't exist). */
    @Transactional(readOnly = true)
    public PayrollReport.EmployeeHours forEmployee(Long userId, PayPeriod period) {
        return build(period, LocalDateTime.now()).getEmployees().stream()
                .filter(e -> e.getUser().getId().equals(userId))
                .findFirst()
                .orElseGet(() -> userRepo.findById(userId)
                        .map(u -> new PayrollReport.EmployeeHours(u, buckets(period).size()))
                        .orElse(null));
    }

    PayrollReport build(PayPeriod period, LocalDateTime now) {
        List<PayrollReport.Bucket> buckets = buckets(period);
        PayrollReport report = new PayrollReport(period, buckets);

        // Start at the Monday of the first week so overtime sees the whole week.
        LocalDate fetchFrom = weekStart(period.getStart());
        List<TimeEntry> entries = new ArrayList<>(timeEntryRepo.findAllByDateRange(fetchFrom, period.getEnd()));
        entries.sort(Comparator.comparing(TimeEntry::getClockInTime));

        // Everyone who's paid hourly, plus anyone else who clocked time in the period.
        Map<Long, PayrollReport.EmployeeHours> byUser = new LinkedHashMap<>();
        List<User> people = new ArrayList<>(userRepo.findByAccountType(AccountType.TECHNICIAN));
        people.addAll(userRepo.findByAccountType(AccountType.MANAGER));
        for (User u : people) byUser.put(u.getId(), new PayrollReport.EmployeeHours(u, buckets.size()));

        // Running minutes per user per week, for overtime.
        Map<Long, Map<LocalDate, Long>> weekMinutes = new HashMap<>();

        for (TimeEntry entry : entries) {
            User user = entry.getUser();
            if (user == null || entry.getClockInTime() == null) continue;

            List<PayrollReport.Shift.Flag> flags = new ArrayList<>();
            long minutes = minutesFor(entry, now, flags);

            Map<LocalDate, Long> weeks = weekMinutes.computeIfAbsent(user.getId(), k -> new HashMap<>());
            LocalDate week = weekStart(entry.getDate());
            long before = weeks.getOrDefault(week, 0L);
            long regular = Math.max(0, Math.min(minutes, overtimeMinutesPerWeek - before));
            weeks.put(week, before + minutes);

            if (!period.contains(entry.getDate())) continue;

            PayrollReport.EmployeeHours eh = byUser.computeIfAbsent(user.getId(),
                    k -> new PayrollReport.EmployeeHours(user, buckets.size()));
            eh.addShift(new PayrollReport.Shift(entry, minutes, minutes - regular, flags),
                    bucketIndex(buckets, entry.getDate()));
        }

        // Shifts in a period are oldest first; people are listed by name.
        byUser.values().stream()
                .sorted(Comparator.comparing(e -> e.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(report::add);
        return report;
    }

    /** Paid minutes for one entry, adding any flags it deserves. */
    static long minutesFor(TimeEntry entry, LocalDateTime now, List<PayrollReport.Shift.Flag> flags) {
        LocalDateTime out = entry.getClockOutTime();
        if (out == null) {
            if (entry.getDate().isBefore(now.toLocalDate())) {
                // Left open from an earlier day: don't guess, count nothing until it's fixed.
                flags.add(PayrollReport.Shift.Flag.MISSING_CLOCK_OUT);
                return 0;
            }
            flags.add(PayrollReport.Shift.Flag.ACTIVE);
            out = now;
        }
        long minutes = Math.max(0, Duration.between(entry.getClockInTime(), out).toMinutes());
        if (entry.getNotes() != null && entry.getNotes().contains("Auto clocked out")) {
            flags.add(PayrollReport.Shift.Flag.AUTO_CLOCKED_OUT);
        }
        if (minutes > LONG_SHIFT_MINUTES) {
            flags.add(PayrollReport.Shift.Flag.LONG_SHIFT);
        }
        return minutes;
    }

    static List<PayrollReport.Bucket> buckets(PayPeriod period) {
        List<PayrollReport.Bucket> out = new ArrayList<>();
        if (period.getDays() <= DAILY_COLUMNS_UP_TO) {
            DateTimeFormatter dow = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
            DateTimeFormatter md = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
            for (LocalDate d = period.getStart(); !d.isAfter(period.getEnd()); d = d.plusDays(1)) {
                out.add(new PayrollReport.Bucket(d, d, d.format(dow), d.format(md)));
            }
            return out;
        }
        DateTimeFormatter md = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        LocalDate s = period.getStart();
        int n = 1;
        while (!s.isAfter(period.getEnd())) {
            LocalDate e = s.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (e.isAfter(period.getEnd())) e = period.getEnd();
            String range = s.equals(e) ? s.format(md) : s.format(md) + "–" + (s.getMonth() == e.getMonth()
                    ? String.valueOf(e.getDayOfMonth()) : e.format(md));
            out.add(new PayrollReport.Bucket(s, e, "Week " + n++, range));
            s = e.plusDays(1);
        }
        return out;
    }

    private static int bucketIndex(List<PayrollReport.Bucket> buckets, LocalDate d) {
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).contains(d)) return i;
        }
        return -1;
    }

    private static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
