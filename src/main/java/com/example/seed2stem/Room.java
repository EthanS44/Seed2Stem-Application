package com.example.seed2stem;

import java.util.Arrays;
import java.util.List;

/**
 * Fixed list of grow rooms. B1 and B2 are flowering rooms, so the filter-change
 * due/overdue flags apply to them. B3 is the veg room and is only offered on the
 * Transfer to Veg and Destruction tasks. Adding a room is a code change by design.
 */
public enum Room {
    B1("B1", true),
    B2("B2", true),
    B3("B3", false);

    private final String displayName;
    private final boolean flowering;

    Room(String displayName, boolean flowering) {
        this.displayName = displayName;
        this.flowering = flowering;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFlowering() {
        return flowering;
    }

    /** Rooms offered in a task's room dropdown: B3 only on Transfer to Veg and Destruction. */
    public static List<Room> forAction(BatchAction action) {
        if (action == BatchAction.TRANSFER_TO_VEG || action == BatchAction.DESTRUCTION) return List.of(values());
        return Arrays.stream(values()).filter(Room::isFlowering).toList();
    }
}
