package com.example.seed2stem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByStatus(BatchStatus status);

    List<Batch> findByStatusNot(BatchStatus status);

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.status NOT IN ('PACKAGED', 'DESTROYED')")
    long countActive();

    @Query("SELECT MAX(b.id) FROM Batch b WHERE YEAR(b.startDate) = :year")
    Long findMaxIdForYear(int year);

    List<Batch> findAllByOrderByCreatedAtDesc();

    List<Batch> findByRoom(String room);

    @Query("SELECT b FROM Batch b WHERE (b.startDate BETWEEN :start AND :end) OR (b.harvestDate BETWEEN :start AND :end)")
    List<Batch> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
