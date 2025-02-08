package io.github.djd.outbox.storage;

import static io.github.djd.outbox.model.MongoOutboxMessageFields.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import io.github.djd.outbox.model.MessageProcessingStatus;
import io.github.djd.outbox.model.OutboxMessage;
public class OutboxUtil {
	
	// Static method to convert Document to OutboxMessage
    public static OutboxMessage fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        return new OutboxMessage(
                doc.getString(ID),  // `_id` is a string
                doc.getString(TOPIC),
                doc.getString(PAYLOAD),
                MessageProcessingStatus.valueOf(doc.getString(STATUS).toUpperCase()), // Convert string to enum
                doc.getDate(CREATED_AT),
                new Date(TimeUnit.MILLISECONDS.toMillis(doc.getLong(PROCESSED_AT))), // processedAt stored in milliseconds
                doc.getString(PROCESSED_BY),
                doc.getInteger(RETRY_COUNT, 0) // Default to 0 if missing
        );
    }

}
