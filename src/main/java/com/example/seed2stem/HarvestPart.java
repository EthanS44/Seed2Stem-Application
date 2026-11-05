package com.example.seed2stem;

/**
 * Harvest happens in two tasks: the tops of the plants first, then the bottoms.
 * Both add wet weight; a plant only counts as harvested once its bottoms are in.
 */
public enum HarvestPart {
    TOPS("Tops"),
    BOTTOMS("Bottoms");

    private final String displayName;

    HarvestPart(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
