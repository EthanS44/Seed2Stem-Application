package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "checklist_response")
public class ChecklistResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean booleanAnswer;

    @Column(columnDefinition = "TEXT")
    private String textAnswer;

    private Double numericAnswer;

    @ManyToOne
    @JoinColumn(name = "checklist_run_id")
    private ChecklistRun checklistRun;

    @ManyToOne
    @JoinColumn(name = "checklist_item_id")
    private ChecklistItem checklistItem;

    public void setBooleanAnswer(Boolean booleanAnswer) {
        this.booleanAnswer = booleanAnswer;
    }
    public void setTextAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }

    public void setChecklistItem(ChecklistItem checklistItem) {
        this.checklistItem = checklistItem;
    }
    public void setNumericAnswer(Double numericAnswer) {
        this.numericAnswer = numericAnswer;
    }
    public void setChecklistRun(ChecklistRun checklistRun) {
        this.checklistRun = checklistRun;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public Boolean getBooleanAnswer() {
        return booleanAnswer;
    }
    public String getTextAnswer() {
        return textAnswer;
    }
    public ChecklistRun getChecklistRun() {
        return checklistRun;
    }
    public ChecklistItem getChecklistItem() {
        return checklistItem;
    }
}

