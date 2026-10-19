package com.example.seed2stem;

/**
 * What a standard task does to the batch record. Chosen per task in the task
 * builder; the run form adds this action's inputs (plus room and batch) on top
 * of any ordinary checklist questions.
 */
public enum BatchAction {
    NONE("None", "Ordinary checklist; nothing is written to a batch record."),
    RECEIVE_SEEDS("Receive seeds",
            "Asks for variety, seed count, supplier or source, lot number and destination room. Creates batch YYMMDD-VVV-Room."),
    CLONING("Cloning",
            "Asks for variety, mother plant or source batch, clones cut and destination room. The first entry of the day for a variety and room creates the batch; later entries join it."),
    TRANSFER_TO_FLOWER("Transfer to flower",
            "Asks for the flowering room and plants moved. Records \"X plants transferred to B1 for flowering\"."),
    FILTER_CHANGE("Clean and change filters",
            "Asks for filters cleaned or changed, number of filters, MERV rating (8 minimum) and notes. Shows days since the room's last change; due after 7 days, overdue after 14."),
    HARVEST("Harvest",
            "Asks for plants cut down and their whole-plant wet weight (kg)."),
    DEBUCK_DESTEM("Debucking and destemming",
            "Asks for plants processed, fan leaf weight and stem weight (kg). Weights are held as material awaiting destruction."),
    TRIMMING("Trimming",
            "Asks for trim (sugar leaf) weight and amount of flower trimmed (kg). Trim is held as material awaiting destruction."),
    DRY_WEIGHT("Weigh dried flower",
            "Asks for final dried flower weight (kg). Calculates drying and processing loss."),
    DESTRUCTION("Destruction",
            "Asks for material type, weight, plant count for whole plants, method and witness. Can't exceed material on hand.");

    private final String displayName;
    private final String description;

    BatchAction(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** Seeds and cloning start a new batch instead of adding to an existing one. */
    public boolean isCreatesBatch() {
        return this == RECEIVE_SEEDS || this == CLONING;
    }
}
