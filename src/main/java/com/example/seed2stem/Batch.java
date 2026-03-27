package com.example.seed2stem;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String batchCode; // Format: YY-DDD-AAA-RR-316-CC
    
    @Column(nullable = false)
    private Integer year; // Last two digits (e.g., 26 for 2026)
    
    @Column(nullable = false)
    private Integer dayOfYear; // 1 thru 365
    
    @Column(nullable = false)
    private String strainName;
    
    @Column(nullable = false)
    private String strainAcronym;
    
    @Column(nullable = false)
    private String destinationRoom; // B1, B2, or B3
    
    @Column(nullable = false)
    private Integer cropNumber; // Sequential number per year
    
    private Integer numberOfClones;
    
    private LocalDate createdDate;
    
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.CLONING;

    // Constructors
    public Batch() {
    }

    public Batch(Integer year, Integer dayOfYear, String strainName, 
                 String strainAcronym, String destinationRoom, Integer cropNumber) {
        this.year = year;
        this.dayOfYear = dayOfYear;
        this.strainName = strainName;
        this.strainAcronym = strainAcronym;
        this.destinationRoom = destinationRoom;
        this.cropNumber = cropNumber;
        this.batchCode = generateBatchCode();
        this.createdDate = LocalDate.now();
    }

    // Generate batch code: YY-DDD-AAA-RR-316-CC
    private String generateBatchCode() {
        return String.format("%02d-%03d-%s-%s-316-%02d",
                year, dayOfYear, strainAcronym, destinationRoom, cropNumber);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
        this.batchCode = generateBatchCode();
    }

    public Integer getDayOfYear() {
        return dayOfYear;
    }

    public void setDayOfYear(Integer dayOfYear) {
        this.dayOfYear = dayOfYear;
        this.batchCode = generateBatchCode();
    }

    public String getStrainName() {
        return strainName;
    }

    public void setStrainName(String strainName) {
        this.strainName = strainName;
    }

    public String getStrainAcronym() {
        return strainAcronym;
    }

    public void setStrainAcronym(String strainAcronym) {
        this.strainAcronym = strainAcronym;
        this.batchCode = generateBatchCode();
    }

    public String getDestinationRoom() {
        return destinationRoom;
    }

    public void setDestinationRoom(String destinationRoom) {
        this.destinationRoom = destinationRoom;
        this.batchCode = generateBatchCode();
    }

    public Integer getCropNumber() {
        return cropNumber;
    }

    public void setCropNumber(Integer cropNumber) {
        this.cropNumber = cropNumber;
        this.batchCode = generateBatchCode();
    }

    public Integer getNumberOfClones() {
        return numberOfClones;
    }

    public void setNumberOfClones(Integer numberOfClones) {
        this.numberOfClones = numberOfClones;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }
}
