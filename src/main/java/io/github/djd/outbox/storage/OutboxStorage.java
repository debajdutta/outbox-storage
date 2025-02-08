package io.github.djd.outbox.storage;

import java.util.List;

import io.github.djd.outbox.model.OutboxMessage;

/**
 * Interface representing the storage mechanism for an Outbox pattern.
 * This interface abstracts the underlying database implementation and 
 * provides methods to persist, retrieve, and mark messages as processed.
 * <p>
 */
public interface OutboxStorage {

    /**
     * Persists a new message in the outbox storage.
     * This method ensures that the message is stored in a durable manner
     * so it can be later processed and published to a messaging system.
     *
     * @param message The {@link OutboxMessage} to be stored.
     */
    public void saveMessage(OutboxMessage message);

    /**
     * Retrieves a batch of unprocessed messages from the outbox storage.
     * The messages returned by this method are typically those that have
     * not yet been marked as processed.
     *
     * @param batchSize The maximum number of messages to fetch in a single call.
     * @return A list of {@link OutboxMessage} instances that are yet to be processed.
     */
    public List<OutboxMessage> fetchUnprocessedMessages(int batchSize);

    /**
     * Marks a message as processed in the outbox storage.
     * Once a message has been successfully published to the messaging system,
     * it should be marked as processed to prevent duplicate processing.
     *
     * @param messageId The unique identifier of the message to be marked as processed.
     */
    public void markMessageAsProcessed(String messageId);
    
    
    public void markMessageAsFailed(String messageId, int maxRetries);
}
