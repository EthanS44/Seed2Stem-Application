package com.example.seed2stem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** A pay period: a type (weekly, bi-weekly...) and the inclusive dates it covers. */
public class PayPeriod {

    public enum Type {
        WEEK("Weekly", "Week"),
        BIWEEKLY("Bi-weekly", "2 weeks"),
        SEMIMONTHLY("Semi-monthly", "Semi"),
        MONTH("Monthly", "Month"),
        CUSTOM("Custom", "Custom");

        private final String displayName;
        private final String shortName;

        Type(String displayName, String shortName) {
            this.displayName = displayName;
            this.shortName = shortName;
        }

        public String getDisplayName() { return displayName; }
        /** Label for the phone-width period switcher. */
        public String getShortName() { return shortName; }
        public String getParam() { return name().toLowerCase(Locale.ROOT); }

        static Type fromParam(String p) {
            if (p == null) return WEEK;
            for (Type t : values()) {
                if (t.getParam().equalsIgnoreCase(p)) return t;
            }
            return WEEK;
        }
    }

    /** Longest custom range we'll report on in one go. */
    static final int MAX_CUSTOM_DAYS = 366;

    private final Type type;
    private final LocalDate start;
    private final LocalDate end;

    PayPeriod(Type type, LocalDate start, LocalDate end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    /**
     * The period of {@code type} that contains {@code date}. Weeks run Monday to Sunday;
     * bi-weekly periods count in 14-day steps from {@code biweeklyAnchor}.
     */
    static PayPeriod containing(Type type, LocalDate date, LocalDate biweeklyAnchor) {
        return switch (type) {
            case WEEK -> {
                LocalDate s = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new PayPeriod(type, s, s.plusDays(6));
            }
            case BIWEEKLY -> {
                long steps = Math.floorDiv(ChronoUnit.DAYS.between(biweeklyAnchor, date), 14);
                LocalDate s = biweeklyAnchor.plusDays(steps * 14);
                yield new PayPeriod(type, s, s.plusDays(13));
            }
            case SEMIMONTHLY -> date.getDayOfMonth() <= 15
                    ? new PayPeriod(type, date.withDayOfMonth(1), date.withDayOfMonth(15))
                    : new PayPeriod(type, date.withDayOfMonth(16), date.with(TemporalAdjusters.lastDayOfMonth()));
            case MONTH -> new PayPeriod(type, date.withDayOfMonth(1), date.with(TemporalAdjusters.lastDayOfMonth()));
            case CUSTOM -> new PayPeriod(type, date, date);
        };
    }

    /** A custom range; the dates are swapped if reversed and clipped to {@link #MAX_CUSTOM_DAYS}. */
    static PayPeriod custom(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            LocalDate t = start; start = end; end = t;
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_CUSTOM_DAYS) {
            end = start.plusDays(MAX_CUSTOM_DAYS - 1);
        }
        return new PayPeriod(Type.CUSTOM, start, end);
    }

    PayPeriod previous(LocalDate biweeklyAnchor) {
        if (type == Type.CUSTOM) {
            long len = getDays();
            return new PayPeriod(type, start.minusDays(len), start.minusDays(1));
        }
        return containing(type, start.minusDays(1), biweeklyAnchor);
    }

    PayPeriod next(LocalDate biweeklyAnchor) {
        if (type == Type.CUSTOM) {
            long len = getDays();
            return new PayPeriod(type, end.plusDays(1), end.plusDays(len));
        }
        return containing(type, end.plusDays(1), biweeklyAnchor);
    }

    public Type getType() { return type; }
    public LocalDate getStart() { return start; }
    public LocalDate getEnd() { return end; }
    public long getDays() { return ChronoUnit.DAYS.between(start, end) + 1; }

    public boolean contains(LocalDate d) {
        return !d.isBefore(start) && !d.isAfter(end);
    }

    public boolean isCurrent() { return contains(LocalDate.now()); }

    /** "Oct 26 – Nov 1, 2026", "Oct 1 – 15, 2026" or "October 2026". */
    public String getLabel() {
        if (type == Type.MONTH) {
            return start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }
        DateTimeFormatter md = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        if (start.getYear() != end.getYear()) {
            return start.format(md) + ", " + start.getYear() + " – " + end.format(md) + ", " + end.getYear();
        }
        if (start.getMonth() == end.getMonth()) {
            return start.format(md) + " – " + end.getDayOfMonth() + ", " + end.getYear();
        }
        return start.format(md) + " – " + end.format(md) + ", " + end.getYear();
    }

    /** Query string that reopens this period, e.g. {@code period=week&date=2026-10-26}. */
    public String getQuery() {
        String q = "period=" + type.getParam();
        return type == Type.CUSTOM
                ? q + "&start=" + start + "&end=" + end
                : q + "&date=" + start;
    }
}
