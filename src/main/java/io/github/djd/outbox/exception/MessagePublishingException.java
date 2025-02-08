package io.github.djd.outbox.exception;

import io.github.djd.outbox.publisher.MessagePublisher;

/**
 * Exception thrown when a message fails to be published to the messaging system.
 * <p>
 * This exception is used to indicate failures in the {@link MessagePublisher} 
 * when attempting to send messages to external systems like Kafka, RabbitMQ, 
 * Amazon SQS, Azure Service Bus, etc.
 * </p>
 * <p>
 * The exception may wrap the underlying cause of the failure, providing 
 * additional context for debugging and error handling.
 * </p>
 *
 * @author DJD
 * @version 1.0
 */
public class MessagePublishingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	/**
     * Constructs a new {@code MessagePublishingException} with the specified detail message.
     *
     * @param message The detail message explaining the reason for the failure.
     */
    public MessagePublishingException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MessagePublishingException} with the specified detail message 
     * and cause.
     *
     * @param message The detail message explaining the reason for the failure.
     * @param cause   The underlying cause of the exception.
     */
    public MessagePublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
