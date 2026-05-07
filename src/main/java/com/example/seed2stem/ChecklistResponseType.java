package com.example.seed2stem;

import java.util.List;

public enum ChecklistResponseType {
    BOOLEAN_TEXT("Pass/Fail"),
    TEXT("Text"),
    INTEGER("Number"),
    NUMBER("Number"),
    DECIMAL("Number"),
    NONE("Checkbox");

    private final String displayName;

    ChecklistResponseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The response types offered in the developer's checklist builder UI. */
    public static List<ChecklistResponseType> selectableForBuilder() {
        return List.of(BOOLEAN_TEXT, TEXT, NUMBER, NONE);
    }
}
