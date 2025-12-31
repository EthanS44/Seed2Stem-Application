package com.example.seed2stem;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "checklist_run")
public class ChecklistRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "completed_by_user_id", nullable = false)
    private User completedBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "authorized_by_user_id")
    private User authorizedBy; // manager/admin

    private LocalDateTime authorizedAt;

    @Enumerated(EnumType.STRING)
    private ChecklistRunStatus status = ChecklistRunStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL)
    private List<ChecklistResponse> responses;

    public void setCompletedAt(LocalDateTime completedAt) {
        this.startTime = completedAt;
    }

    public void setStatus(ChecklistRunStatus checklistRunStatus) {
        this.status = checklistRunStatus;
    }
    public void setTask(Task task) {
        this.task = task;
    }

    public ChecklistRunStatus getStatus() {
        return status;
    }
    public void setResponses(List<ChecklistResponse> responses) {
        this.responses = responses;
    }
    public void setCompletedBy(User completedBy) {
        this.completedBy = completedBy;
    }
    public void setAuthorizedBy(User authorizedBy) {
        this.authorizedBy = authorizedBy;
    }
    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }
    public Long getId() {
        return id;
    }
    public User getCompletedBy() {
        return completedBy;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public User getAuthorizedBy() {
        return authorizedBy;
    }
    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }
    public List<ChecklistResponse> getResponses() {
        return responses;
    }

}