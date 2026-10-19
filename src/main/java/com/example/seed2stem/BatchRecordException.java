package com.example.seed2stem;

/** A batch record input or approval failed a rule; the message is shown to the user. */
public class BatchRecordException extends RuntimeException {
    public BatchRecordException(String message) {
        super(message);
    }
}
