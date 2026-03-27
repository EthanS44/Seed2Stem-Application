package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trimming_session")
public class TrimmingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @OneToMany(mappedBy = "trimmingSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrimmingWorkerEntry> workerEntries = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    private Boolean isCompleted = false; // Set to true on the final session

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public TrimmingSession() {
    }

    public TrimmingSession(Batch batch, LocalDate sessionDate) {
        this.batch = batch;
        this.sessionDate = sessionDate;
    }

    // Helper method to calculate total weight for the session
    public Double getTotalWeightTrimmed() {
        return workerEntries.stream()
                .mapToDouble(TrimmingWorkerEntry::getWeightTrimmedInGrams)
                .sum();
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

    public List<TrimmingWorkerEntry> getWorkerEntries() {
        return workerEntries;
    }

    public void setWorkerEntries(List<TrimmingWorkerEntry> workerEntries) {
        this.workerEntries = workerEntries;
    }

    public void addWorkerEntry(TrimmingWorkerEntry entry) {
        workerEntries.add(entry);
        entry.setTrimmingSession(this);
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
