package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepo;
    private final ChecklistRunRepository runRepo;

    public TimeEntryService(TimeEntryRepository timeEntryRepo, ChecklistRunRepository runRepo) {
        this.timeEntryRepo = timeEntryRepo;
        this.runRepo = runRepo;
    }

    @Transactional
    public TimeEntry clockIn(User user) {
        Optional<TimeEntry> active = timeEntryRepo.findByUserAndClockOutTimeIsNull(user);
        if (active.isPresent()) {
            throw new RuntimeException("You are already clocked in");
        }

        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setDate(LocalDate.now());
        entry.setClockInTime(LocalDateTime.now());
        return timeEntryRepo.save(entry);
    }

    @Transactional
    public TimeEntry clockOut(User user, String notes) {
        TimeEntry entry = timeEntryRepo.findByUserAndClockOutTimeIsNull(user)
                .orElseThrow(() -> new RuntimeException("You are not currently clocked in"));

        // Every task (paused ones included) has to be submitted before the shift can end.
        List<ChecklistRun> open = runRepo.findByCompletedByAndStatus(user, ChecklistRunStatus.IN_PROGRESS);
        if (!open.isEmpty()) {
            throw new RuntimeException("Finish your open tasks before clocking out: "
                    + open.stream().map(ChecklistRun::getChecklistName).collect(Collectors.joining(", ")));
        }

        entry.setClockOutTime(LocalDateTime.now());
        entry.setTotalHours(computeHours(entry.getClockInTime(), entry.getClockOutTime()));
        if (notes != null && !notes.isBlank()) {
            entry.setNotes(notes);
        }
        return timeEntryRepo.save(entry);
    }

    public Optional<TimeEntry> getActiveEntry(User user) {
        return timeEntryRepo.findByUserAndClockOutTimeIsNull(user);
    }

    public boolean isClockedIn(User user) {
        return timeEntryRepo.findByUserAndClockOutTimeIsNull(user).isPresent();
    }

    public List<TimeEntry> getTodayEntries(User user) {
        return timeEntryRepo.findByUserAndDateOrderByClockInTimeDesc(user, LocalDate.now());
    }

    public List<TimeEntry> getEntriesForDateRange(User user, LocalDate start, LocalDate end) {
        return timeEntryRepo.findByUserAndDateBetweenOrderByDateDescClockInTimeDesc(user, start, end);
    }

    public List<TimeEntry> getAllEntriesForDateRange(LocalDate start, LocalDate end) {
        return timeEntryRepo.findAllByDateRange(start, end);
    }

    public List<TimeEntry> getAllActiveEntries() {
        return timeEntryRepo.findAllActive();
    }

    public long countClockedIn() {
        return timeEntryRepo.countCurrentlyClockedIn();
    }

    @Transactional
    public TimeEntry updateEntry(Long entryId, LocalDateTime clockIn, LocalDateTime clockOut, String notes) {
        TimeEntry entry = timeEntryRepo.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Time entry not found"));

        if (clockOut != null && clockOut.isBefore(clockIn)) {
            throw new RuntimeException("Clock out time cannot be before clock in time");
        }
        checkNoOverlap(entry.getUser(), entryId, clockIn, clockOut);

        entry.setDate(clockIn.toLocalDate());
        entry.setClockInTime(clockIn);
        entry.setClockOutTime(clockOut);
        if (clockOut != null) {
            entry.setTotalHours(computeHours(clockIn, clockOut));
        }
        if (notes != null) {
            entry.setNotes(notes);
        }
        return timeEntryRepo.save(entry);
    }

    /** A finished shift entered by a manager, e.g. when someone forgot to clock in. */
    @Transactional
    public TimeEntry addEntry(User user, LocalDateTime clockIn, LocalDateTime clockOut, String notes) {
        if (clockOut == null) {
            throw new RuntimeException("Enter a clock out time");
        }
        if (!clockOut.isAfter(clockIn)) {
            throw new RuntimeException("Clock out time must be after clock in time");
        }
        if (clockOut.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Shifts can't end in the future");
        }
        checkNoOverlap(user, null, clockIn, clockOut);

        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setDate(clockIn.toLocalDate());
        entry.setClockInTime(clockIn);
        entry.setClockOutTime(clockOut);
        entry.setTotalHours(computeHours(clockIn, clockOut));
        entry.setNotes(notes);
        return timeEntryRepo.save(entry);
    }

    /** Shifts can't overlap, and only one can be open at a time. */
    private void checkNoOverlap(User user, Long ignoreId, LocalDateTime in, LocalDateTime out) {
        LocalDateTime end = out != null ? out : LocalDateTime.now();
        List<TimeEntry> nearby = timeEntryRepo.findByUserAndDateBetweenOrderByDateDescClockInTimeDesc(
                user, in.toLocalDate().minusDays(1), end.toLocalDate());
        for (TimeEntry other : nearby) {
            if (ignoreId != null && ignoreId.equals(other.getId())) continue;
            if (out == null && other.getClockOutTime() == null) {
                throw new RuntimeException("They already have an open shift. Add a clock out time");
            }
            LocalDateTime otherEnd = other.getClockOutTime() != null ? other.getClockOutTime() : LocalDateTime.now();
            if (in.isBefore(otherEnd) && other.getClockInTime().isBefore(end)) {
                DateTimeFormatter time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                throw new RuntimeException("It overlaps their "
                        + other.getClockInTime().format(time) + " – "
                        + (other.getClockOutTime() != null ? other.getClockOutTime().format(time) : "now")
                        + " shift on " + other.getDate().format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH))
                        + ". Fix or delete that one first.");
            }
        }
    }

    @Transactional
    public int autoClockOutStaleEntries() {
        List<TimeEntry> stale = timeEntryRepo.findStaleEntries(LocalDate.now());
        for (TimeEntry entry : stale) {
            LocalDateTime endOfDay = LocalDateTime.of(entry.getDate(), LocalTime.of(23, 59));
            entry.setClockOutTime(endOfDay);
            entry.setTotalHours(computeHours(entry.getClockInTime(), endOfDay));
            entry.setNotes((entry.getNotes() != null ? entry.getNotes() + " | " : "") + "Auto clocked out");
            timeEntryRepo.save(entry);
        }
        return stale.size();
    }

    private double computeHours(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}
