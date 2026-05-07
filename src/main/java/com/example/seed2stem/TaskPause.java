package com.example.seed2stem;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_pause")
public class TaskPause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "checklist_run_id", nullable = false)
    private ChecklistRun checklistRun;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String reason;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public ChecklistRun getChecklistRun() {
        return checklistRun;
    }
    public void setChecklistRun(ChecklistRun checklistRun) {
        this.checklistRun = checklistRun;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
}
