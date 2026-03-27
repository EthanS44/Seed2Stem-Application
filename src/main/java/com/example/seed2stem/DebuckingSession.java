package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "debucking_session")
public class DebuckingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private Double weightDebuckedInGrams;

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    private Boolean isCompleted = false; // Set to true on the final session

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public DebuckingSession() {
    }

    public DebuckingSession(Batch batch, LocalDate sessionDate, Double weightDebuckedInGrams,
                           User performedBy) {
        this.batch = batch;
        this.sessionDate = sessionDate;
        this.weightDebuckedInGrams = weightDebuckedInGrams;
        this.performedBy = performedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Double getWeightDebuckedInGrams() {
        return weightDebuckedInGrams;
    }

    public void setWeightDebuckedInGrams(Double weightDebuckedInGrams) {
        this.weightDebuckedInGrams = weightDebuckedInGrams;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public ChecklistRun getChecklistRun() {
        return checklistRun;
    }

    public void setChecklistRun(ChecklistRun checklistRun) {
        this.checklistRun = checklistRun;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
