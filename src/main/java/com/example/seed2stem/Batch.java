package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String batchCode;

    @Column(nullable = false)
    private String strain;

    @Column(nullable = false)
    private String strainAcronym;

    @Column(nullable = false)
    private String room; // B1, B2, B3

    @Column(nullable = false)
    private Integer plantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.CLONING;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate harvestDate;

    private Double harvestWeightGrams;

    private Double coaSampleWeightGrams;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private LocalDateTime createdAt;

    public Batch() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public String getStrain() { return strain; }
    public void setStrain(String strain) { this.strain = strain; }

    public String getStrainAcronym() { return strainAcronym; }
    public void setStrainAcronym(String strainAcronym) { this.strainAcronym = strainAcronym; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public Integer getPlantCount() { return plantCount; }
    public void setPlantCount(Integer plantCount) { this.plantCount = plantCount; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getHarvestDate() { return harvestDate; }
    public void setHarvestDate(LocalDate harvestDate) { this.harvestDate = harvestDate; }

    public Double getHarvestWeightGrams() { return harvestWeightGrams; }
    public void setHarvestWeightGrams(Double harvestWeightGrams) { this.harvestWeightGrams = harvestWeightGrams; }

    public Double getCoaSampleWeightGrams() { return coaSampleWeightGrams; }
    public void setCoaSampleWeightGrams(Double coaSampleWeightGrams) { this.coaSampleWeightGrams = coaSampleWeightGrams; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
