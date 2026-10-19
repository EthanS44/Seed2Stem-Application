package com.example.seed2stem;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A grow batch. Created when a Receive Seeds or Cloning entry is approved;
 * every later task adds approved BatchRecordEntry rows to it. Totals are
 * derived from those entries, never stored here.
 */
@Entity
@Table(name = "grow_batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** YYMMDD-VVV-Room, e.g. 261008-ABC-B1. */
    @Column(nullable = false, unique = true)
    private String batchCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private Variety variety;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private BatchSource source;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public Variety getVariety() { return variety; }
    public void setVariety(Variety variety) { this.variety = variety; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public BatchSource getSource() { return source; }
    public void setSource(BatchSource source) { this.source = source; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
