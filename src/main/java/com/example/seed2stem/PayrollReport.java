package com.example.seed2stem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Hours for everyone in one pay period, built by {@link PayrollService}. All times are in minutes. */
public class PayrollReport {

    private final PayPeriod period;
    private final List<Bucket> buckets;
    private final List<EmployeeHours> employees = new ArrayList<>();
    private long previousTotalMinutes;
    private boolean comparedToPartial;

    PayrollReport(PayPeriod period, List<Bucket> buckets) {
        this.period = period;
        this.buckets = buckets;
    }

    void add(EmployeeHours e) { employees.add(e); }
    void setPreviousTotalMinutes(long m) { previousTotalMinutes = m; }
    void setComparedToPartial(boolean b) { comparedToPartial = b; }

    public PayPeriod getPeriod() { return period; }
    public List<Bucket> getBuckets() { return buckets; }
    public List<EmployeeHours> getEmployees() { return employees; }

    public long getTotalMinutes() { return employees.stream().mapToLong(EmployeeHours::getTotalMinutes).sum(); }
    public long getRegularMinutes() { return employees.stream().mapToLong(EmployeeHours::getRegularMinutes).sum(); }
    public long getOvertimeMinutes() { return employees.stream().mapToLong(EmployeeHours::getOvertimeMinutes).sum(); }
    public long getPreviousTotalMinutes() { return previousTotalMinutes; }
    /** True when the comparison only covers the previous period up to the same day. */
    public boolean isComparedToPartial() { return comparedToPartial; }
    public long getShiftCount() { return employees.stream().mapToLong(e -> e.getShifts().size()).sum(); }
    public long getEmployeesWorked() { return employees.stream().filter(e -> e.getTotalMinutes() > 0).count(); }
    public long getOvertimeCount() { return employees.stream().filter(e -> e.getOvertimeMinutes() > 0).count(); }
    public long getFlaggedCount() { return employees.stream().mapToLong(EmployeeHours::getFlaggedCount).sum(); }
    public long getClockedInCount() { return employees.stream().filter(EmployeeHours::isClockedIn).count(); }

    /** Change against the previous period, as a whole percentage; null when there's nothing to compare. */
    public Long getChangePercent() {
        if (previousTotalMinutes == 0) return null;
        return Math.round((getTotalMinutes() - previousTotalMinutes) * 100.0 / previousTotalMinutes);
    }

    /** Team minutes in bucket {@code i}. */
    public long bucketTotal(int i) {
        return employees.stream().mapToLong(e -> e.getBucketMinutes()[i]).sum();
    }

    /** The busiest bucket, for scaling the team chart. */
    public long getMaxBucketTotal() {
        long max = 0;
        for (int i = 0; i < buckets.size(); i++) max = Math.max(max, bucketTotal(i));
        return max;
    }

    /** Decimal hours, as payroll systems take them: 450 min → "7.50". */
    public static String hours(long minutes) {
        return String.format("%.2f", minutes / 60.0);
    }

    /** One column of the hours grid: a day for short periods, a (clipped) week for long ones. */
    public static class Bucket {
        private final LocalDate start;
        private final LocalDate end;
        private final String label;
        private final String subLabel;

        Bucket(LocalDate start, LocalDate end, String label, String subLabel) {
            this.start = start;
            this.end = end;
            this.label = label;
            this.subLabel = subLabel;
        }

        public LocalDate getStart() { return start; }
        public LocalDate getEnd() { return end; }
        public String getLabel() { return label; }
        public String getSubLabel() { return subLabel; }
        public boolean isToday() { LocalDate t = LocalDate.now(); return !t.isBefore(start) && !t.isAfter(end); }
        boolean contains(LocalDate d) { return !d.isBefore(start) && !d.isAfter(end); }
    }

    public static class EmployeeHours {
        private final User user;
        private final long[] bucketMinutes;
        private final List<Shift> shifts = new ArrayList<>();
        private long regularMinutes;
        private long overtimeMinutes;
        private boolean clockedIn;

        EmployeeHours(User user, int bucketCount) {
            this.user = user;
            this.bucketMinutes = new long[bucketCount];
        }

        void addShift(Shift s, int bucket) {
            shifts.add(s);
            regularMinutes += s.getRegularMinutes();
            overtimeMinutes += s.getOvertimeMinutes();
            if (bucket >= 0) bucketMinutes[bucket] += s.getMinutes();
            if (s.isActive()) clockedIn = true;
        }

        public User getUser() { return user; }
        public long[] getBucketMinutes() { return bucketMinutes; }
        public long bucket(int i) { return bucketMinutes[i]; }
        public List<Shift> getShifts() { return shifts; }
        public long getRegularMinutes() { return regularMinutes; }
        public long getOvertimeMinutes() { return overtimeMinutes; }
        public long getTotalMinutes() { return regularMinutes + overtimeMinutes; }
        public boolean isClockedIn() { return clockedIn; }
        public long getDaysWorked() { return shifts.stream().map(Shift::getDate).distinct().count(); }
        public long getFlaggedCount() { return shifts.stream().filter(Shift::isFlagged).count(); }
    }

    /** One time entry inside the period, with the overtime it carries. */
    public static class Shift {
        public enum Flag {
            ACTIVE("On the clock", "info"),
            MISSING_CLOCK_OUT("No clock-out", "bad"),
            AUTO_CLOCKED_OUT("Auto clocked out", "warn"),
            LONG_SHIFT("Over 12 h", "warn");

            private final String label;
            private final String tone;

            Flag(String label, String tone) { this.label = label; this.tone = tone; }

            public String getLabel() { return label; }
            public String getTone() { return tone; }
            /** Flags a manager should look at before running payroll (being on the clock isn't one). */
            public boolean isProblem() { return this != ACTIVE; }
        }

        private final TimeEntry entry;
        private final long minutes;
        private final long overtimeMinutes;
        private final List<Flag> flags;

        Shift(TimeEntry entry, long minutes, long overtimeMinutes, List<Flag> flags) {
            this.entry = entry;
            this.minutes = minutes;
            this.overtimeMinutes = overtimeMinutes;
            this.flags = flags;
        }

        public TimeEntry getEntry() { return entry; }
        public LocalDate getDate() { return entry.getDate(); }
        public LocalDateTime getClockIn() { return entry.getClockInTime(); }
        public LocalDateTime getClockOut() { return entry.getClockOutTime(); }
        public long getMinutes() { return minutes; }
        public long getOvertimeMinutes() { return overtimeMinutes; }
        public long getRegularMinutes() { return minutes - overtimeMinutes; }
        public List<Flag> getFlags() { return flags; }
        public boolean isActive() { return flags.contains(Flag.ACTIVE); }
        public boolean isFlagged() { return flags.stream().anyMatch(Flag::isProblem); }
    }
}
