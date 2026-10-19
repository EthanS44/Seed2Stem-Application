package com.example.seed2stem;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One line on a batch record: the batch-specific inputs of a single task run.
 * Saved as DRAFT on pause, PENDING on submit, and only counts toward the batch
 * once the run is APPROVED. For seeds/cloning the batch doesn't exist until
 * approval, so {@link #proposedBatchCode} carries the number in the meantime.
 */
@Entity
@Table(name = "batch_record_entry")
public class BatchRecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "checklist_run_id", nullable = false, unique = true)
    private ChecklistRun run;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private BatchAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private BatchEntryStatus status = BatchEntryStatus.DRAFT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "technician_user_id", nullable = false)
    private User technician;

    @Column(nullable = false)
    private LocalDate entryDate;

    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private Room room;

    private String proposedBatchCode;

    // Variety (seeds/cloning): an existing one, or a new name+code created on approval.
    @ManyToOne
    @JoinColumn(name = "variety_id")
    private Variety variety;
    private String newVarietyName;
    private String newVarietyCode;

    /** Seeds received, clones cut, plants moved/harvested/processed, or whole plants destroyed. */
    private Integer plantCount;

    /** Wet weight (harvest), dry flower weight, or weight destroyed — in kg. */
    private Double weightKg;
    private Double fanLeafKg;
    private Double stemKg;
    private Double trimKg;
    private Double flowerTrimmedKg;

    /** Supplier/source (seeds) or mother plant/source batch (cloning). */
    private String source;
    private String lotNumber;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(30)")
    private FilterWork filterWork;
    private Integer filterCount;
    private Integer filterRating;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(30)")
    private DestructionMaterial material;
    private String destructionMethod;
    private String witness;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChecklistRun getRun() { return run; }
    public void setRun(ChecklistRun run) { this.run = run; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public BatchAction getAction() { return action; }
    public void setAction(BatchAction action) { this.action = action; }
    public BatchEntryStatus getStatus() { return status; }
    public void setStatus(BatchEntryStatus status) { this.status = status; }
    public User getTechnician() { return technician; }
    public void setTechnician(User technician) { this.technician = technician; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getProposedBatchCode() { return proposedBatchCode; }
    public void setProposedBatchCode(String proposedBatchCode) { this.proposedBatchCode = proposedBatchCode; }
    public Variety getVariety() { return variety; }
    public void setVariety(Variety variety) { this.variety = variety; }
    public String getNewVarietyName() { return newVarietyName; }
    public void setNewVarietyName(String newVarietyName) { this.newVarietyName = newVarietyName; }
    public String getNewVarietyCode() { return newVarietyCode; }
    public void setNewVarietyCode(String newVarietyCode) { this.newVarietyCode = newVarietyCode; }
    public Integer getPlantCount() { return plantCount; }
    public void setPlantCount(Integer plantCount) { this.plantCount = plantCount; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public Double getFanLeafKg() { return fanLeafKg; }
    public void setFanLeafKg(Double fanLeafKg) { this.fanLeafKg = fanLeafKg; }
    public Double getStemKg() { return stemKg; }
    public void setStemKg(Double stemKg) { this.stemKg = stemKg; }
    public Double getTrimKg() { return trimKg; }
    public void setTrimKg(Double trimKg) { this.trimKg = trimKg; }
    public Double getFlowerTrimmedKg() { return flowerTrimmedKg; }
    public void setFlowerTrimmedKg(Double flowerTrimmedKg) { this.flowerTrimmedKg = flowerTrimmedKg; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public FilterWork getFilterWork() { return filterWork; }
    public void setFilterWork(FilterWork filterWork) { this.filterWork = filterWork; }
    public Integer getFilterCount() { return filterCount; }
    public void setFilterCount(Integer filterCount) { this.filterCount = filterCount; }
    public Integer getFilterRating() { return filterRating; }
    public void setFilterRating(Integer filterRating) { this.filterRating = filterRating; }
    public DestructionMaterial getMaterial() { return material; }
    public void setMaterial(DestructionMaterial material) { this.material = material; }
    public String getDestructionMethod() { return destructionMethod; }
    public void setDestructionMethod(String destructionMethod) { this.destructionMethod = destructionMethod; }
    public String getWitness() { return witness; }
    public void setWitness(String witness) { this.witness = witness; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /** Variety name for display, whether existing or pending creation. */
    public String getVarietyLabel() {
        if (variety != null) return variety.getName() + " (" + variety.getCode() + ")";
        if (newVarietyName != null) return newVarietyName + " (" + newVarietyCode + ") — new";
        return batch != null ? batch.getVariety().getName() : null;
    }

    /** The weight column of the batch record log, in kg. */
    public Double getLogWeightKg() {
        return switch (action) {
            case DEBUCK_DESTEM -> sum(fanLeafKg, stemKg);
            case TRIMMING -> trimKg;
            default -> weightKg;
        };
    }

    /** One-line description of the action-specific inputs for the batch record log. */
    public String getDetails() {
        String kg = "%.3f kg";
        return switch (action) {
            case RECEIVE_SEEDS -> join("Seeds received", source != null ? "from " + source : null,
                    lotNumber != null ? "lot " + lotNumber : null);
            case CLONING -> join("Clones cut", source != null ? "from " + source : null);
            case TRANSFER_TO_FLOWER -> plantCount + " plants transferred to " + (room != null ? room.getDisplayName() : "?")
                    + " for flowering";
            case FILTER_CHANGE -> join(filterWork != null ? "Filters " + filterWork.getDisplayName().toLowerCase() : null,
                    filterCount != null ? filterCount + " filters" : null,
                    filterRating != null ? "MERV " + filterRating : null, notes);
            case HARVEST -> "Wet weight (whole plant)";
            case DEBUCK_DESTEM -> join(fanLeafKg != null ? "Fan leaves " + String.format(kg, fanLeafKg) : null,
                    stemKg != null ? "stems " + String.format(kg, stemKg) : null);
            case TRIMMING -> join("Trim (sugar leaves)",
                    flowerTrimmedKg != null ? "flower trimmed " + String.format(kg, flowerTrimmedKg) : null);
            case DRY_WEIGHT -> "Final dried flower";
            case DESTRUCTION -> join(material != null ? material.getDisplayName() + " destroyed" : "Destroyed",
                    destructionMethod != null ? "by " + destructionMethod : null,
                    witness != null ? "witness " + witness : null);
            default -> notes;
        };
    }

    private static Double sum(Double a, Double b) {
        if (a == null && b == null) return null;
        return (a != null ? a : 0.0) + (b != null ? b : 0.0);
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    /** The batch number shown on the record: the real batch once approved, else the proposed one. */
    public String getBatchCodeLabel() {
        return batch != null ? batch.getBatchCode() : proposedBatchCode;
    }
}
