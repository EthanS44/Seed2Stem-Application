package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_activity_log")
public class BatchActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Enumerated(EnumType.STRING)
    private BatchStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus toStatus;

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public BatchActivityLog() {}

    public BatchActivityLog(Batch batch, BatchStatus fromStatus, BatchStatus toStatus,
                            User performedBy, String notes) {
        this.batch = batch;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.performedBy = performedBy;
        this.notes = notes;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Batch getBatch() { return batch; }
    public BatchStatus getFromStatus() { return fromStatus; }
    public BatchStatus getToStatus() { return toStatus; }
    public User getPerformedBy() { return performedBy; }
    public String getNotes() { return notes; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setBatch(Batch batch) { this.batch = batch; }
    public void setFromStatus(BatchStatus fromStatus) { this.fromStatus = fromStatus; }
    public void setToStatus(BatchStatus toStatus) { this.toStatus = toStatus; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
