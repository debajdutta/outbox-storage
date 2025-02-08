package io.github.djd.outbox.processor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.djd.outbox.exception.MessagePublishingException;
import io.github.djd.outbox.model.OutboxMessage;
import io.github.djd.outbox.publisher.MessagePublisher;
import io.github.djd.outbox.storage.OutboxStorage;

public class PollingProcessor implements MessageProcessor {
	
    private final OutboxStorage storage;
    private final MessagePublisher publisher;
    private ScheduledExecutorService executor;
    private volatile boolean running = true;
    
    // retry parameters
    private final int pollingIntervalMs;
    private final int maxRetries;
    private static final int POLLING_BATCH_SIZE =10;
    
    //logger
    private static final Logger logger = LoggerFactory.getLogger(PollingProcessor.class);
    
    public PollingProcessor(OutboxStorage storage, MessagePublisher publisher, int pollingIntervalMs, int maxRetries) {
        this.storage = storage;
        this.publisher = publisher;
        this.pollingIntervalMs = pollingIntervalMs;
        this.maxRetries = maxRetries;
    }
    
    @Override
    public void startProcessing() {
    	executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::processMessages, 0, pollingIntervalMs, TimeUnit.MILLISECONDS);
        logger.info("PollingProcessor started.");
    }

    private void processMessages() {
    	while (running) {
            try {
                List<OutboxMessage> messages = storage.fetchUnprocessedMessages(POLLING_BATCH_SIZE);
                for (OutboxMessage message : messages) {
                    processMessageWithRetries(message);
                }
                Thread.sleep(pollingIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("PollingProcessor interrupted, shutting down...");
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in PollingProcessor: ", e);
            }
        }
    }
    
    private void processMessageWithRetries(OutboxMessage message) {
    	logger.trace("processMessageWithRetries");
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                publisher.publish(message);
                storage.markMessageAsProcessed(message.getId());
                return;
            } catch (MessagePublishingException e) {
                attempts++;
                logger.error("Failed to publish message: {}  (Attempt {}/{})", message.getId(), attempts, maxRetries);
                if (attempts == maxRetries) {
                    logger.error("Max retries reached. Moving message to DLQ or alerting...");
                    // Mark message as failed (retry or mark as permanently failed)
                    storage.markMessageAsFailed(message.getId(), maxRetries);
                }
            }
        }
    }
    
    @Override
    public void stopProcessing() {
    	running = false;
    	try {
			publisher.close();
			if (executor != null && !executor.isShutdown()) {
	            executor.shutdown();
	            logger.info("PollingProcessor stopped.");
	        }
		} catch (Exception e) {
			logger.error("Error stopping PollingProcessor. ", e);
		}        
    }
}
