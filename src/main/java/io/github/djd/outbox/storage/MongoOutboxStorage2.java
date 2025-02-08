package io.github.djd.outbox.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import io.github.djd.outbox.model.OutboxMessage;

public class MongoOutboxStorage2 implements OutboxStorage {

	private final MongoCollection<OutboxMessage> outboxCollection;
	private final String instanceId;
	//private static final int BATCH_SIZE = 10; // Adjust batch size as needed

	private static final Logger logger = LoggerFactory.getLogger(MongoOutboxStorage2.class);
	
	public MongoOutboxStorage2(MongoCollection<OutboxMessage> outboxCollection) {
		this.outboxCollection = outboxCollection;
		this.instanceId = UUID.randomUUID().toString(); // Unique ID for this service instance
	}

	@Override
	public void saveMessage(OutboxMessage message) {
//		Document doc = new Document("_id", message.getId())
//				.append("topic", message.getTopic())
//				.append("payload", message.getPayload())
//				.append("processed", false)
//				.append("createdAt", new Date());
		outboxCollection.insertOne(message);
		logger.info("Added message with ID {}.", message.getId());
	}

//	@Override
//	public List<OutboxMessage> fetchUnprocessedMessages(int batchSize) {
//		List<OutboxMessage> messages = 
//				outboxCollection.find(Filters.eq("status", "PENDING"))
//                .limit(batchSize)
//                .into(new java.util.ArrayList<>());
//		logger.info("Fetched {} unprocessed messages.", messages.size());
//		return messages;
//	}

	/**
	 * Fetch a batch of messages and mark them as PROCESSING atomically
	 *  
	 * @return List of locked messages
	 */
	@Override
	public List<OutboxMessage> fetchUnprocessedMessages(int batchSize) {
	    List<OutboxMessage> lockedMessages = new ArrayList<>();

	    for (int i = 0; i < batchSize; i++) {
	        OutboxMessage message = outboxCollection.findOneAndUpdate(
	            Filters.eq("status", "PENDING"),  // Pick only unprocessed messages
	            Updates.combine(
	                Updates.set("status", "PROCESSING"),  // Lock the message
	                Updates.set("processingAt", System.currentTimeMillis()),
	                Updates.set("lockedBy", instanceId) // Track the instance
	            ),
	            new FindOneAndUpdateOptions()
	                .sort(Sorts.ascending("createdAt"))  // Prioritize older messages
	                .returnDocument(ReturnDocument.AFTER)  // Return updated doc
	        );

	        if (message == null) {
	            break;  // No more messages to process
	        }
	        lockedMessages.add(message);
	    }
	    
	    logger.info("Fetched {} unprocessed messages.", lockedMessages.size());
	    
	    return lockedMessages;
	}

	/**
	 * Mark message as PROCESSED after successful publishing
	 * @param messageId Unique message ID
	 */
	public void markMessageAsProcessed(String messageId) {
		outboxCollection.updateOne(
				Filters.eq("_id", messageId),
				Updates.set("status", "PROCESSED")
				);
		logger.debug("Updated message with ID {}, set status to PROCESSED.", messageId);
	}

	/**
	 * Handle failures by retrying or marking the message as FAILED
	 * @param messageId Unique message ID
	 * @param maxRetries Maximum retry attempts
	 */
	public void markMessageAsFailed(String messageId, int maxRetries) {
		OutboxMessage message = outboxCollection.find(Filters.eq("_id", messageId)).first();
		if (message != null) {
			int currentRetries = message.getRetryCount();//("retry_count", 0);
			if (currentRetries >= maxRetries) {
				outboxCollection.updateOne(
						Filters.eq("_id", messageId),
						Updates.set("status", "FAILED")
						);
			} else {
				outboxCollection.updateOne(
						Filters.eq("_id", messageId),
						Updates.combine(
								Updates.set("status", "PENDING"),
								Updates.inc("retryCount", 1)
								)
						);
			}
			logger.debug("Processed message with ID {}, set status to FAILED.", messageId);
		}
	}
}
