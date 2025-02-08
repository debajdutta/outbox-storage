package io.github.djd.outbox.model;

public enum MessageProcessingStatus {
    PENDING,    // Message is waiting to be processed
    IN_PROGRESS, // Message processing has started
    COMPLETED,  // Message processing is successfully completed
    FAILED      // Message processing has failed
}
