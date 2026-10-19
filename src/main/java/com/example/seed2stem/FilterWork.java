package com.example.seed2stem;

public enum FilterWork {
    CLEANED("Cleaned"),
    CHANGED("Changed"),
    CLEANED_AND_CHANGED("Cleaned and changed");

    private final String displayName;

    FilterWork(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
