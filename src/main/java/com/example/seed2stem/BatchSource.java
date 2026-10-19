package com.example.seed2stem;

public enum BatchSource {
    SEEDS("Seeds"),
    CLONES("Clones");

    private final String displayName;

    BatchSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
