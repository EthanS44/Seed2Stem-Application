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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistResponseType responseType;

    @Enumerated(EnumType.STRING)
    private ChecklistItemCategory category;

    private Integer displayOrder;
    private Integer questionOrder;

    @ManyToOne
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public ChecklistItemType getItemType() {
        return itemType;
    }
    public void setItemType(ChecklistItemType itemType) {
        this.itemType = itemType;
    }
    public ChecklistResponseType getResponseType() {
        return responseType;
    }
    public void setResponseType(ChecklistResponseType responseType) {
        this.responseType = responseType;
    }
    public ChecklistItemCategory getCategory() {
        return category;
    }
    public void setCategory(ChecklistItemCategory category) {
        this.category = category;
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
    public Checklist getChecklist() {
        return checklist;
    }
    public void setChecklist(Checklist checklist) {
        this.checklist = checklist;
    }
}
