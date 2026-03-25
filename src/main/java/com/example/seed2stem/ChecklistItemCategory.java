package com.example.seed2stem;

public enum ChecklistItemCategory {
    LIGHTS("Lights"),
    FANS("Fans"),
    HUMIDITY("Humidity"),
    TEMPERATURE("Temperature"),
    WATER_LEVEL("Water Level"),
    EC("EC"),
    PH("pH"),
    NUTRIENTS("Nutrients"),
    PLANTS("Light Plants"),
    EMITTERS("Missing Emitters"),
    WATER_LEAKS("Water Leaks"),
    TABLE_ALIGNMENT("Table Alignment"),
    WILTING_LEAVES("Wilting Leaves"),
    GENERAL("General");

    private final String displayName;

    ChecklistItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
