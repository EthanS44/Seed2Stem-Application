package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepo;

    public TimeEntryService(TimeEntryRepository timeEntryRepo) {
        this.timeEntryRepo = timeEntryRepo;
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
