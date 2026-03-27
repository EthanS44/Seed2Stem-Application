package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "destruction_record")
public class DestructionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate destructionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestructionType destructionType; // LIVE_PLANTS or DRY_PRODUCT

    // Use only one of these based on destructionType
    private Integer numberOfPlants; // For live plants
    private Double weightInGrams; // For dry product

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    // Constructors
    public DestructionRecord() {
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

    public LocalDate getDestructionDate() {
        return destructionDate;
    }

    public void setDestructionDate(LocalDate destructionDate) {
        this.destructionDate = destructionDate;
    }

    public DestructionType getDestructionType() {
        return destructionType;
    }

    public void setDestructionType(DestructionType destructionType) {
        this.destructionType = destructionType;
    }

    public Integer getNumberOfPlants() {
        return numberOfPlants;
    }

    public void setNumberOfPlants(Integer numberOfPlants) {
        this.numberOfPlants = numberOfPlants;
    }

    public Double getWeightInGrams() {
        return weightInGrams;
    }

    public void setWeightInGrams(Double weightInGrams) {
        this.weightInGrams = weightInGrams;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ChecklistRun getChecklistRun() {
        return checklistRun;
    }

    public void setChecklistRun(ChecklistRun checklistRun) {
        this.checklistRun = checklistRun;
    }
}
