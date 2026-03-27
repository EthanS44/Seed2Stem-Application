package com.example.seed2stem;

import jakarta.persistence.*;

@Entity
@Table(name = "packaging_entry")
public class PackagingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "packaging_session_id", nullable = false)
    private PackagingSession packagingSession;

    @Column(nullable = false)
    private Double bagSizeInGrams; // e.g., 500, 1000, 2000

    @Column(nullable = false)
    private Integer numberOfBags;

    // For the last uneven bag
    private Double partialBagWeightInGrams;

    // Constructors
    public PackagingEntry() {
    }

    public PackagingEntry(Double bagSizeInGrams, Integer numberOfBags) {
        this.bagSizeInGrams = bagSizeInGrams;
        this.numberOfBags = numberOfBags;
    }

    public PackagingEntry(Double bagSizeInGrams, Integer numberOfBags, Double partialBagWeightInGrams) {
        this.bagSizeInGrams = bagSizeInGrams;
        this.numberOfBags = numberOfBags;
        this.partialBagWeightInGrams = partialBagWeightInGrams;
    }

    // Calculate total weight for this entry
    public Double getTotalWeight() {
        double total = bagSizeInGrams * numberOfBags;
        if (partialBagWeightInGrams != null) {
            total += partialBagWeightInGrams;
        }
        return total;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PackagingSession getPackagingSession() {
        return packagingSession;
    }

    public void setPackagingSession(PackagingSession packagingSession) {
        this.packagingSession = packagingSession;
    }

    public Double getBagSizeInGrams() {
        return bagSizeInGrams;
    }

    public void setBagSizeInGrams(Double bagSizeInGrams) {
        this.bagSizeInGrams = bagSizeInGrams;
    }

    public Integer getNumberOfBags() {
        return numberOfBags;
    }

    public void setNumberOfBags(Integer numberOfBags) {
        this.numberOfBags = numberOfBags;
    }

    public Double getPartialBagWeightInGrams() {
        return partialBagWeightInGrams;
    }

    public void setPartialBagWeightInGrams(Double partialBagWeightInGrams) {
        this.partialBagWeightInGrams = partialBagWeightInGrams;
    }
}
