package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "checklist_item")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String question;

    private String responseType; // BOOLEAN, TEXT, NUMBER
    private Integer itemOrder;
    private Boolean required;

    @ManyToOne
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    // getters and setters
}

