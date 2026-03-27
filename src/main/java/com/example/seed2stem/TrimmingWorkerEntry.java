package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "trimming_worker_entry")
public class TrimmingWorkerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trimming_session_id", nullable = false)
    private TrimmingSession trimmingSession;

    @ManyToOne
    @JoinColumn(name = "worker_user_id", nullable = false)
    private User worker;

    @Column(nullable = false)
    private Double weightTrimmedInGrams;

    // Constructors
    public TrimmingWorkerEntry() {
    }

    public TrimmingWorkerEntry(User worker, Double weightTrimmedInGrams) {
        this.worker = worker;
        this.weightTrimmedInGrams = weightTrimmedInGrams;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrimmingSession getTrimmingSession() {
        return trimmingSession;
    }

    public void setTrimmingSession(TrimmingSession trimmingSession) {
        this.trimmingSession = trimmingSession;
    }

    public User getWorker() {
        return worker;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }

    public Double getWeightTrimmedInGrams() {
        return weightTrimmedInGrams;
    }

    public void setWeightTrimmedInGrams(Double weightTrimmedInGrams) {
        this.weightTrimmedInGrams = weightTrimmedInGrams;
    }
}
