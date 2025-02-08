package io.github.djd.outbox.model;
public class MongoOutboxMessageFields {
    public static final String ID = "_id";
    public static final String TOPIC = "messageTopic";
    public static final String PAYLOAD = "messagePayload";
    public static final String STATUS = "processingStatus";
    public static final String CREATED_AT = "createdAt";
    public static final String PROCESSED_AT = "processedAt";
    public static final String PROCESSED_BY = "processedByInstance";
    public static final String RETRY_COUNT = "retryCount";
}