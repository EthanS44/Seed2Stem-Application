package com.example.seed2stem;

public enum BatchStatus {
    CLONING("Cloning"),
    VEGETATIVE("Vegetative"),
    FLOWERING("Flowering"),
    HARVESTED("Harvested"),
    DEBUCKING("Debucking"),
    TRIMMING("Trimming"),
    PACKAGING("Packaging"),
    PACKAGED("Packaged"),
    DESTROYED("Destroyed");

    private final String displayName;

    BatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
