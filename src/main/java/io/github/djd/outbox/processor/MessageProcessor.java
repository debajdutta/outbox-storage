package io.github.djd.outbox.processor;

/**
 * Represents a processor that handles the processing of messages from the outbox storage.
 * The processor is responsible for starting and stopping the processing of messages, 
 * which could involve tasks like polling or listening for changes in the outbox.
 */
public interface MessageProcessor {

    /**
     * Starts processing the messages from the outbox storage.
     * The implementation of this method could involve polling the database 
     * or listening to a message queue, depending on the processor type (e.g., Polling, CDC).
     * This method should initiate any background tasks or threads needed to process messages.
     */
    void startProcessing();

    /**
     * Stops the processing of messages.
     * This method should gracefully shut down any background tasks or threads 
     * involved in processing, ensuring that no new messages are processed and
     * that resources are cleaned up as needed.
     */
    void stopProcessing();
}
