package io.github.djd.outbox.storage;

import static io.github.djd.outbox.model.MongoOutboxMessageFields.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import io.github.djd.outbox.model.MessageProcessingStatus;
import io.github.djd.outbox.model.OutboxMessage;

public class MongoOutboxStorage implements OutboxStorage {

	private final MongoCollection<Document> outboxCollection;
	private final String instanceId;

	private static final Logger logger = LoggerFactory.getLogger(MongoOutboxStorage.class);
	
	public MongoOutboxStorage(MongoCollection<Document> outboxCollection) {
		this.outboxCollection = outboxCollection;
		this.instanceId = UUID.randomUUID().toString(); // Unique ID for this service instance
	}

	@Override
	public void saveMessage(OutboxMessage message) {
		
		// Fields to be set: id, topic, payload, status, createdAt;
		
		Document doc = new Document(ID, message.getId())
				.append(TOPIC, message.getTopic())
				.append(PAYLOAD, message.getPayload())
				.append(STATUS, MessageProcessingStatus.PENDING)
				.append(CREATED_AT, new Date())
				.append(RETRY_COUNT, 0);
		outboxCollection.insertOne(doc);
		logger.info("Added message with ID {}.", message.getId());
	}

	/**
	 * Fetch a batch of messages and mark them as PROCESSING atomically
	 *  
	 * @return List of locked messages
	 */
	public List<OutboxMessage> fetchUnprocessedMessages(int count) {
	    List<OutboxMessage> lockedMessages = new ArrayList<>();

	    for (int i = 0; i < count; i++) {
	        Document doc = outboxCollection.findOneAndUpdate(
	            Filters.eq(STATUS, MessageProcessingStatus.PENDING),  // Pick only unprocessed messages
	            Updates.combine(
	                Updates.set(STATUS, MessageProcessingStatus.IN_PROGRESS),  // Lock the message
	                Updates.set(PROCESSED_AT, System.currentTimeMillis()), // Timestamp to track processing
	                Updates.set(PROCESSED_BY, instanceId) // Track which instance is processing
	            ),
	            new FindOneAndUpdateOptions()
	                .sort(Sorts.ascending(CREATED_AT))  // Prioritize older messages
	                .returnDocument(ReturnDocument.AFTER)  // Return updated doc
	        );

	        if (doc == null) {
	            break;  // No more messages to process
	        } else {
	        	OutboxMessage message = OutboxUtil.fromDocument(doc);
	        	if(message!=null) {
	        		lockedMessages.add(message);
	        	}
	        }
	        
	    }
	    logger.info("Fetching {} {} for processing. ", lockedMessages.size(), lockedMessages.size() > 0? "messages": "message");
	    return lockedMessages;
	}

	/**
	 * Mark message as PROCESSED after successful publishing
	 * @param messageId Unique message ID
	 */
	public void markMessageAsProcessed(String messageId) {
		outboxCollection.updateOne(
				Filters.eq(ID, messageId),
				Updates.set(STATUS, MessageProcessingStatus.COMPLETED)
				);
		logger.debug("Updated message with ID {}, set status to {}.", messageId, MessageProcessingStatus.COMPLETED);
	}

	/**
	 * Handle failures by retrying or marking the message as FAILED
	 * @param messageId Unique message ID
	 * @param maxRetries Maximum retry attempts
	 */
	public void markMessageAsFailed(String messageId, int maxRetries) {
		Document message = outboxCollection.find(Filters.eq(ID, messageId)).first();
		if (message != null) {
			int currentRetries = message.getInteger(RETRY_COUNT, 0);
			if (currentRetries >= maxRetries) {
				outboxCollection.updateOne(
						Filters.eq(ID, messageId),
						Updates.set(STATUS, MessageProcessingStatus.FAILED)
						);
				logger.error("Updated message with ID {}, set status to {}.", messageId, MessageProcessingStatus.FAILED);
			} else {
				outboxCollection.updateOne(
						Filters.eq(ID, messageId),
						Updates.combine(
								Updates.set(STATUS, MessageProcessingStatus.PENDING),
								Updates.inc(RETRY_COUNT, 1)
								)
						);
				logger.warn("Updated message with ID {}, retry count incremeted, set status to {}.", messageId, MessageProcessingStatus.FAILED);
			}			
		}
	}
}
