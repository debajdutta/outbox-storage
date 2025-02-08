package io.github.djd.outbox.publisher;

import io.github.djd.outbox.exception.MessagePublishingException;
import io.github.djd.outbox.model.OutboxMessage;

/**
 * Interface representing a message publisher in the Outbox pattern.
 * This interface abstracts the message publishing mechanism and allows 
 * integration with various messaging systems like Kafka, RabbitMQ, 
 * Amazon SQS, Azure Service Bus, etc.
 * <p>
 * Implementations of this interface should handle the process of 
 * serializing and sending messages reliably to the configured messaging system.
 * </p>
 *
 */
public interface MessagePublisher {

	/**
     * Publishes a given message to the configured messaging system.
     * This method should ensure that the message is sent reliably and 
     * handle any failures or retries if necessary.
     *
     * @param message The {@link OutboxMessage} to be published.
     * @throws MessagePublishingException if an error occurs during message publishing.
     */
    void publish(OutboxMessage message) throws MessagePublishingException;

    /**
     * Cleans up any resources used by the publisher (e.g., client, threads etc.).
     */
    void close() throws Exception;
}
