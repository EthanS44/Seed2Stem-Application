package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "checklist_item")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String itemType; // QUESTION, HEADING, TEXT
    private String responseType; // BOOLEAN_TEXT, TEXT, NUMBER, NONE

    private Integer displayOrder;   // visual ordering
    private Integer questionOrder;  // numbering (questions only)

    @ManyToOne
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    public Checklist getChecklist() {
        return checklist;
    }
    public void setChecklist(Checklist checklist) {
        this.checklist = checklist;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getText() {
        return text;
    }
    public void setText(String question) {
        this.text = question;
    }
    public String getResponseType() {
        return responseType;
    }
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
    public Integer getQuestionOrder() {
        return questionOrder;
    }
    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
    public String getItemType() {
        return itemType;
    }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}

