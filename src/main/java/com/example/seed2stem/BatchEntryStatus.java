package com.example.seed2stem;

/**
 * Lifecycle of a batch record entry. Only APPROVED entries count toward a
 * batch's totals; DRAFT holds values while a task is paused.
 */
public enum BatchEntryStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED
}
