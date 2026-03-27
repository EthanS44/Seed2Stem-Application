package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "packaging_session")
public class PackagingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @OneToMany(mappedBy = "packagingSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PackagingEntry> entries = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    private Boolean isCompleted = false; // Set to true on the final session

    // Final trim waste weight (only filled on last session)
    private Double trimWasteWeightInGrams;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public PackagingSession() {
    }

    public PackagingSession(Batch batch, LocalDate sessionDate, User performedBy) {
        this.batch = batch;
        this.sessionDate = sessionDate;
        this.performedBy = performedBy;
    }

    // Helper method to calculate total weight for the session
    public Double getTotalWeightPackaged() {
        return entries.stream()
                .mapToDouble(PackagingEntry::getTotalWeight)
                .sum();
    }

    public void addEntry(PackagingEntry entry) {
        entries.add(entry);
        entry.setPackagingSession(this);
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

    public List<PackagingEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PackagingEntry> entries) {
        this.entries = entries;
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

    public Double getTrimWasteWeightInGrams() {
        return trimWasteWeightInGrams;
    }

    public void setTrimWasteWeightInGrams(Double trimWasteWeightInGrams) {
        this.trimWasteWeightInGrams = trimWasteWeightInGrams;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
