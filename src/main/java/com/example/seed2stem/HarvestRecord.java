package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "harvest_record")
public class HarvestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate harvestDate;

    @Column(nullable = false)
    private Integer numberOfPlantsHarvested;

    @Column(nullable = false)
    private Double totalWeightInGrams;

    @Column(nullable = false)
    private Double coaSampleWeightInGrams; // Certificate of Analysis sample weight

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public HarvestRecord() {
    }

    public HarvestRecord(Batch batch, LocalDate harvestDate, Integer numberOfPlantsHarvested,
                        Double totalWeightInGrams, Double coaSampleWeightInGrams, User performedBy) {
        this.batch = batch;
        this.harvestDate = harvestDate;
        this.numberOfPlantsHarvested = numberOfPlantsHarvested;
        this.totalWeightInGrams = totalWeightInGrams;
        this.coaSampleWeightInGrams = coaSampleWeightInGrams;
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

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public Integer getNumberOfPlantsHarvested() {
        return numberOfPlantsHarvested;
    }

    public void setNumberOfPlantsHarvested(Integer numberOfPlantsHarvested) {
        this.numberOfPlantsHarvested = numberOfPlantsHarvested;
    }

    public Double getTotalWeightInGrams() {
        return totalWeightInGrams;
    }

    public void setTotalWeightInGrams(Double totalWeightInGrams) {
        this.totalWeightInGrams = totalWeightInGrams;
    }

    public Double getCoaSampleWeightInGrams() {
        return coaSampleWeightInGrams;
    }

    public void setCoaSampleWeightInGrams(Double coaSampleWeightInGrams) {
        this.coaSampleWeightInGrams = coaSampleWeightInGrams;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
