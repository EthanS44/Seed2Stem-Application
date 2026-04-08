package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    Optional<TimeEntry> findByUserAndClockOutTimeIsNull(User user);

    List<TimeEntry> findByUserAndDateOrderByClockInTimeDesc(User user, LocalDate date);

    List<TimeEntry> findByUserAndDateBetweenOrderByDateDescClockInTimeDesc(
            User user, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM TimeEntry t JOIN FETCH t.user WHERE t.date BETWEEN :start AND :end ORDER BY t.date DESC, t.clockInTime DESC")
    List<TimeEntry> findAllByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM TimeEntry t JOIN FETCH t.user WHERE t.clockOutTime IS NULL")
    List<TimeEntry> findAllActive();

    @Query("SELECT COUNT(t) FROM TimeEntry t WHERE t.clockOutTime IS NULL")
    long countCurrentlyClockedIn();

    @Query("SELECT t FROM TimeEntry t WHERE t.clockOutTime IS NULL AND t.date < :today")
    List<TimeEntry> findStaleEntries(@Param("today") LocalDate today);
}
