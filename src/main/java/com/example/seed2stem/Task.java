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

    /** Original filename of the uploaded SOP PDF; null when no SOP is attached. */
    @Column(name = "sop_file_name")
    private String sopFileName;

    /**
     * The raw PDF bytes for the SOP, stored as a PostgreSQL `bytea` column
     * (NOT a Large Object — `oid`/LO requires explicit transactions and breaks
     * read-only/auto-commit queries). Marked @Basic(fetch = LAZY) as a hint;
     * note that real lazy loading of basic byte[] fields requires bytecode
     * enhancement, so without it Hibernate may load this eagerly.
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "sop_data", columnDefinition = "bytea")
    private byte[] sopData;

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
    public String getSopFileName() {
        return sopFileName;
    }
    public void setSopFileName(String sopFileName) {
        this.sopFileName = sopFileName;
    }
    public byte[] getSopData() {
        return sopData;
    }
    public void setSopData(byte[] sopData) {
        this.sopData = sopData;
    }
}
