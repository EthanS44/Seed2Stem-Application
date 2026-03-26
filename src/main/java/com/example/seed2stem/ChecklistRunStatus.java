package com.example.seed2stem;

public enum ChecklistRunStatus {
    IN_PROGRESS, // technician is filling it out
    PENDING,     // submitted, waiting for manager approval
    APPROVED,    // manager authorized
    REJECTED     // manager rejected, technician must redo
}
