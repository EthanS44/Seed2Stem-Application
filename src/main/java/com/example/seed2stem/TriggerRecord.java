package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "trigger_record")
public class TriggerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private LocalDate triggerDate;

    @Column(nullable = false)
    private Integer numberOfPlants;

    @ManyToOne
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    // Link to checklist run if using checklist system
    @OneToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    // Constructors
    public TriggerRecord() {
    }

    public TriggerRecord(Batch batch, LocalDate triggerDate, Integer numberOfPlants, User performedBy) {
        this.batch = batch;
        this.triggerDate = triggerDate;
        this.numberOfPlants = numberOfPlants;
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

    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(LocalDate triggerDate) {
        this.triggerDate = triggerDate;
    }

    public Integer getNumberOfPlants() {
        return numberOfPlants;
    }

    public void setNumberOfPlants(Integer numberOfPlants) {
        this.numberOfPlants = numberOfPlants;
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
}
