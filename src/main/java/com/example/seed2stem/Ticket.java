package com.example.seed2stem;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A bug / feature-request ticket submitted by any user. Developers see all
 * tickets in /developer/tickets and can mark them resolved. Submitters fire
 * and forget — they don't get any visibility into status after submission.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private User submittedBy;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'OPEN'")
    private TicketStatus status = TicketStatus.OPEN;

    private LocalDateTime resolvedAt;

    public Ticket() {
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
    public User getSubmittedBy() {
        return submittedBy;
    }
    public void setSubmittedBy(User submittedBy) {
        this.submittedBy = submittedBy;
    }
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    public TicketStatus getStatus() {
        return status;
    }
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
