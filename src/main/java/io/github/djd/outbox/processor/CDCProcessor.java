package io.github.djd.outbox.processor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.github.djd.outbox.exception.MessagePublishingException;
import io.github.djd.outbox.model.OutboxMessage;
import io.github.djd.outbox.publisher.MessagePublisher;
import io.github.djd.outbox.storage.OutboxStorage;
import io.github.djd.outbox.storage.OutboxUtil;

public class CDCProcessor implements MessageProcessor {
	
	private static final Logger logger = LoggerFactory.getLogger(CDCProcessor.class);
	
    private final OutboxStorage storage;
    private final MessagePublisher publisher;
    private MongoCollection<Document> collection;
    private final ExecutorService executor;
    private volatile boolean running = true;
    
    // Retry parameters
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_TIME_MS = 1000; // 1 second
    private static final long MAX_BACKOFF_TIME_MS = 30000; // 30 seconds

    public CDCProcessor(OutboxStorage storage, MessagePublisher publisher, MongoCollection<Document> collection) {
        this.storage = storage;
        this.publisher = publisher;
        this.collection = collection;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void startProcessing() {
    	logger.trace("startProcessing");
        executor.submit(() -> {
            try (MongoClient client = MongoClients.create()) {
                MongoDatabase database = client.getDatabase(collection.getNamespace().getDatabaseName());
                MongoCollection<Document> outboxCollection = database.getCollection(collection.getNamespace().getCollectionName());

                // Listen for new messages in the outbox table using MongoDB Change Streams.
                try (MongoCursor<ChangeStreamDocument<Document>> cursor = outboxCollection.watch().iterator()) {
                	logger.info("Processing started.");
                    while (running && cursor.hasNext()) {
                        ChangeStreamDocument<Document> change = cursor.next();
                        Document doc = change.getFullDocument();
                        
                        if (doc != null) {
                            OutboxMessage message = OutboxUtil.fromDocument(doc);
                            
                            // Retry logic for message publishing
                            boolean success = false;
                            int retries = 0;
                            while (!success && retries < MAX_RETRIES) {
                                try {
                                    publisher.publish(message);
                                    storage.markMessageAsProcessed(message.getId()); // Mark message as processed
                                    success = true; // Publish successful
                                } catch (MessagePublishingException e) {
                                    retries++;
                                    long backoffTime = Math.min(INITIAL_BACKOFF_TIME_MS * (1L << retries), MAX_BACKOFF_TIME_MS);
                                    logger.error("Failed to publish message with ID: {}. Attempt {} of {}. Retrying in {} ms...", message.getId(), retries, MAX_RETRIES, backoffTime, e);
                                    storage.markMessageAsFailed(message.getId(), MAX_RETRIES);
                                    
                                    // Sleep before retrying
                                    try {
                                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        logger.error("Retry sleep was interrupted.", ie);
                                    }
                                }
                            }

                            if (!success) {
                            	logger.error("Failed to publish message with ID: {} after {} retries.", message.getId(), MAX_RETRIES);
                                // Optionally: Add message to a dead-letter queue (DLQ) or further failure handling.
                            }
                        }
                    }
                }
            } catch (MongoException e) {
            	logger.error("MongoDB error while processing change stream: " + e.getMessage());
            } catch (Exception e) {
            	logger.error("Unexpected error during CDC processing: " + e.getMessage());
            }
        });
    }

    @Override
    public void stopProcessing() {
    	logger.trace("stopProcessing");
        running = false;
        try {
			publisher.close();
			if (executor != null && !executor.isShutdown()) {
	            executor.shutdown();
	            logger.info("Processing stopped.");
	        }
		} catch (Exception e) {
			logger.error("Error stopping CDCProcessor. ", e);		
		}        
    }
    
    
}
