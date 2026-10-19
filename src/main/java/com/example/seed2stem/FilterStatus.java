package com.example.seed2stem;

import java.time.LocalDate;

/**
 * When a room's filters were last cleaned or changed. Flowering rooms are
 * flagged due after 7 days and overdue after 14.
 */
public class FilterStatus {

    public static final int DUE_AFTER_DAYS = 7;
    public static final int OVERDUE_AFTER_DAYS = 14;

    private final Room room;
    private final LocalDate lastChange;
    private final Long daysSince;

    public FilterStatus(Room room, LocalDate lastChange, LocalDate today) {
        this.room = room;
        this.lastChange = lastChange;
        this.daysSince = lastChange != null ? java.time.temporal.ChronoUnit.DAYS.between(lastChange, today) : null;
    }

    public Room getRoom() { return room; }
    public LocalDate getLastChange() { return lastChange; }
    public Long getDaysSince() { return daysSince; }

    /** OK, DUE, OVERDUE, or NONE when nothing has been recorded. */
    public String getState() {
        if (daysSince == null) return "NONE";
        if (!room.isFlowering()) return "OK";
        if (daysSince > OVERDUE_AFTER_DAYS) return "OVERDUE";
        if (daysSince > DUE_AFTER_DAYS) return "DUE";
        return "OK";
    }

    public String getSummary() {
        if (lastChange == null) return "No filter change recorded for " + room.getDisplayName();
        String when = daysSince == 0 ? "today" : daysSince == 1 ? "1 day ago" : daysSince + " days ago";
        return switch (getState()) {
            case "OVERDUE" -> "Last changed " + when + " — overdue";
            case "DUE" -> "Last changed " + when + " — due";
            default -> "Last changed " + when;
        };
    }
}
