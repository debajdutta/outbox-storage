package io.github.djd.outbox.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.djd.outbox.model.OutboxMessage;
import io.github.djd.outbox.processor.MessageProcessor;
import io.github.djd.outbox.processor.PollingProcessor;
import io.github.djd.outbox.publisher.MessagePublisher;
import io.github.djd.outbox.storage.OutboxStorage;

/**
 * OutboxService is responsible for managing the processing of messages stored in an outbox.
 * It uses a {@link MessageProcessor} to either poll for new messages or listen to changes 
 * (such as CDC). The service stores messages in a persistent outbox storage and ensures
 * that the messages are processed reliably and efficiently.
 * 
 * This class handles the initialization and management of message processing, including:
 * - Storing messages in the outbox.
 * - Starting message processing through the provided {@link MessageProcessor}.
 * - Ensuring proper shutdown through a JVM shutdown hook.
 */
public class OutboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxStorage storage;
    private MessageProcessor processor;
    private final ExecutorService executorService;
    
    private static final int DEAFULT_POLLING_INTERVAL_IN_MILLI_SEC = 5000;
    private static final int DEFAULT_RETRY = 3;

    /**
     * Constructor to initialize the OutboxService with a custom {@link MessageProcessor}.
     * This constructor also sets up a shutdown hook to ensure proper cleanup when the 
     * application terminates.
     *
     * @param storage The {@link OutboxStorage} used to store messages.
     * @param processor The {@link MessageProcessor} responsible for processing messages.
     */
    public OutboxService(OutboxStorage storage, MessageProcessor processor) {
        this.storage = storage;
        this.processor = processor;
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Register a JVM shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        // Automatically start processing messages
        startProcessing(); 
    }

    /**
     * Factory method to create an instance of {@link OutboxService} with the default 
     * {@link PollingProcessor}.
     * 
     * @param storage The {@link OutboxStorage} used to store messages.
     * @param publisher The {@link MessagePublisher} used to publish messages.
     * @return An instance of {@link OutboxService} initialized with a default {@link PollingProcessor}.
     */
    public static OutboxService withDefaultProcessor(OutboxStorage storage, MessagePublisher publisher) {
        logger.info("Starting the PollingProcessor with polling interval {}ms and retry value {}.", DEAFULT_POLLING_INTERVAL_IN_MILLI_SEC, DEFAULT_RETRY);
        return new OutboxService(storage, new PollingProcessor(storage, publisher, DEAFULT_POLLING_INTERVAL_IN_MILLI_SEC, DEFAULT_RETRY));
    }
    
    /**
     * Stores the provided message in the outbox storage.
     * 
     * @param message The {@link OutboxMessage} to be stored in the outbox.
     */
    public void storeMessage(OutboxMessage message) {
        try {
            storage.saveMessage(message);
            logger.debug("Saved message with ID {}", message.getId());
        } catch (Exception e) {
            logger.error("Failed to store message: ", e);
        }
    }

    /**
     * Starts the message processing in a separate thread using the {@link ExecutorService}.
     * This method submits the {@link MessageProcessor}'s startProcessing method for execution.
     */
    private void startProcessing() {
        logger.trace("startProcessing");
        executorService.submit(processor::startProcessing);
    }

    /**
     * Shuts down the {@link OutboxService} by stopping the message processor and shutting
     * down the {@link ExecutorService}.
     */
    private void shutdown() {
        logger.debug("Shutting down OutboxService...");
        processor.stopProcessing();  // Ensure processor stops
        executorService.shutdownNow();  // Stop executor
        logger.info("OutboxService shutdown successfully.");
    }
}
