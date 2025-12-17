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

    // getters and setters
}

