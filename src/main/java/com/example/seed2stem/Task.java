package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    @Column(columnDefinition = "boolean default false")
    private boolean userCreated;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    public Checklist getChecklist() {
        return checklist;
    }
    public void setChecklist(Checklist checklist) {
        this.checklist = checklist;
    }
    public boolean isUserCreated() {
        return userCreated;
    }
    public void setUserCreated(boolean userCreated) {
        this.userCreated = userCreated;
    }
    public User getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    public Task() {
    }
    public Task(String title, String description, Checklist checklist) {
        this.title = title;
        this.description = description;
        this.checklist = checklist;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
