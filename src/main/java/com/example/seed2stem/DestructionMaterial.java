package com.example.seed2stem;

public enum DestructionMaterial {
    STEMS("Stems"),
    FAN_LEAVES("Fan leaves"),
    TRIM("Sugar leaves / trim"),
    WHOLE_PLANTS("Whole plants");

    private final String displayName;

    DestructionMaterial(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
