package com.example.seed2stem;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Running totals for one batch, derived from its APPROVED entries. Weights are
 * kg. Also computes material on hand awaiting destruction and the drying and
 * processing loss: wet - (fan leaves + stems + trim + dry flower).
 */
public class BatchTotals {

    private int seedsReceived;
    private int clonesCut;
    private int plantsTransferred;
    private int plantsHarvested;
    private int plantsProcessed;
    private int plantsDestroyed;
    private double wetKg;
    private double fanLeafKg;
    private double stemKg;
    private double trimKg;
    private double flowerTrimmedKg;
    private double dryKg;
    private boolean hasDryWeight;
    private double destroyedKg;
    private final Map<DestructionMaterial, Double> destroyedKgByMaterial = new EnumMap<>(DestructionMaterial.class);
    private final Map<Room, Integer> transferredByRoom = new EnumMap<>(Room.class);
    private LocalDate firstTransferDate;
    private LocalDate firstHarvestDate;

    public static BatchTotals of(List<BatchRecordEntry> approvedEntries) {
        BatchTotals t = new BatchTotals();
        for (BatchRecordEntry e : approvedEntries) t.add(e);
        return t;
    }

    private void add(BatchRecordEntry e) {
        int plants = e.getPlantCount() != null ? e.getPlantCount() : 0;
        switch (e.getAction()) {
            case RECEIVE_SEEDS -> seedsReceived += plants;
            case CLONING -> clonesCut += plants;
            case TRANSFER_TO_FLOWER -> {
                plantsTransferred += plants;
                if (e.getRoom() != null) transferredByRoom.merge(e.getRoom(), plants, Integer::sum);
                firstTransferDate = earliest(firstTransferDate, e.getEntryDate());
            }
            case HARVEST -> {
                plantsHarvested += plants;
                wetKg += kg(e.getWeightKg());
                firstHarvestDate = earliest(firstHarvestDate, e.getEntryDate());
            }
            case DEBUCK_DESTEM -> {
                plantsProcessed += plants;
                fanLeafKg += kg(e.getFanLeafKg());
                stemKg += kg(e.getStemKg());
            }
            case TRIMMING -> {
                trimKg += kg(e.getTrimKg());
                flowerTrimmedKg += kg(e.getFlowerTrimmedKg());
            }
            case DRY_WEIGHT -> {
                dryKg += kg(e.getWeightKg());
                hasDryWeight = true;
            }
            case DESTRUCTION -> {
                destroyedKg += kg(e.getWeightKg());
                if (e.getMaterial() != null) {
                    destroyedKgByMaterial.merge(e.getMaterial(), kg(e.getWeightKg()), Double::sum);
                    if (e.getMaterial() == DestructionMaterial.WHOLE_PLANTS) plantsDestroyed += plants;
                }
            }
            default -> { }
        }
    }

    private static double kg(Double v) {
        return v != null ? v : 0.0;
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.isBefore(a) ? b : a;
    }

    public int getPlantsStarted() { return seedsReceived + clonesCut; }

    /** Plants still alive in the batch: started, minus harvested, minus whole plants destroyed. */
    public int getLivePlants() { return Math.max(0, getPlantsStarted() - plantsHarvested - plantsDestroyed); }

    public double getDestroyedKg(DestructionMaterial m) { return destroyedKgByMaterial.getOrDefault(m, 0.0); }

    /** kg recorded but not yet destroyed, for the material types that are weighed beforehand. */
    public double getOnHandKg(DestructionMaterial m) {
        double recorded = switch (m) {
            case STEMS -> stemKg;
            case FAN_LEAVES -> fanLeafKg;
            case TRIM -> trimKg;
            case WHOLE_PLANTS -> 0.0;
        };
        return Math.max(0.0, round3(recorded - getDestroyedKg(m)));
    }

    public double getOnHandStemsKg() { return getOnHandKg(DestructionMaterial.STEMS); }
    public double getOnHandFanLeafKg() { return getOnHandKg(DestructionMaterial.FAN_LEAVES); }
    public double getOnHandTrimKg() { return getOnHandKg(DestructionMaterial.TRIM); }

    /** Loss in kg once a dry weight exists, else null. */
    public Double getLossKg() {
        if (!hasDryWeight) return null;
        return round3(wetKg - (fanLeafKg + stemKg + trimKg + dryKg));
    }

    /** Loss as a percentage of wet weight, else null. */
    public Double getLossPercent() {
        Double loss = getLossKg();
        if (loss == null || wetKg <= 0) return null;
        return Math.round(loss / wetKg * 1000.0) / 10.0;
    }

    public static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public int getSeedsReceived() { return seedsReceived; }
    public int getClonesCut() { return clonesCut; }
    public int getPlantsTransferred() { return plantsTransferred; }
    public int getPlantsHarvested() { return plantsHarvested; }
    public int getPlantsProcessed() { return plantsProcessed; }
    public int getPlantsDestroyed() { return plantsDestroyed; }
    public double getWetKg() { return round3(wetKg); }
    public double getFanLeafKg() { return round3(fanLeafKg); }
    public double getStemKg() { return round3(stemKg); }
    public double getTrimKg() { return round3(trimKg); }
    public double getFlowerTrimmedKg() { return round3(flowerTrimmedKg); }
    public double getDryKg() { return round3(dryKg); }
    public boolean isHasDryWeight() { return hasDryWeight; }
    public double getDestroyedKg() { return round3(destroyedKg); }
    public Map<Room, Integer> getTransferredByRoom() { return transferredByRoom; }
    public LocalDate getFirstTransferDate() { return firstTransferDate; }
    public LocalDate getFirstHarvestDate() { return firstHarvestDate; }

    /** Derived growth stage, from the furthest step recorded. */
    public String getStage() {
        if (hasDryWeight) return "Dried";
        if (plantsHarvested > 0) return "Harvested";
        if (plantsTransferred > 0) return "Flowering";
        if (getPlantsStarted() > 0) return "Vegetative";
        return "Started";
    }
}
