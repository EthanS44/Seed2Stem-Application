package com.example.seed2stem;

/**
 * Fixed list of grow rooms. Both are flowering rooms, so the filter-change
 * due/overdue flags apply to each. Adding a room is a code change by design.
 */
public enum Room {
    B1("B1"),
    B2("B2");

    private final String displayName;

    Room(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFlowering() {
        return true;
    }
}
