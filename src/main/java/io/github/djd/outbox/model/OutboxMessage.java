package io.github.djd.outbox.model;

import java.util.Date;
import java.util.UUID;

public class OutboxMessage {
    private String id;
    private String topic;
    private String payload;
    private MessageProcessingStatus status;
    private Date createdAt;
    private Date processedAt;
    private String processedBy;
    private int retryCount;
    
    public OutboxMessage() {}
    
    public OutboxMessage(String topic, String payload) {
    	this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.status = MessageProcessingStatus.PENDING;
        this.createdAt = new Date();
        this.retryCount = 0;
	}    

	public OutboxMessage(String id, String topic, String payload, MessageProcessingStatus status, Date createdAt,
			Date processedAt, String processedBy, int retryCount) {
		super();
		this.id = id;
		this.topic = topic;
		this.payload = payload;
		this.status = status;
		this.createdAt = createdAt;
		this.processedAt = processedAt;
		this.processedBy = processedBy;
		this.retryCount = retryCount;
	}

	public String getId() {
		return id;
	}

	public String getTopic() {
		return topic;
	}

	public String getPayload() {
		return payload;
	}

	public MessageProcessingStatus getStatus() {
		return status;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public Date getProcessedAt() {
		return processedAt;
	}

	public String getProcessedBy() {
		return processedBy;
	}

	public int getRetryCount() {
		return retryCount;
	}

	// define the setter for the mutable fields
	public void setStatus(MessageProcessingStatus status) {
		this.status = status;
	}

	public void setProcessedAt(Date processedAt) {
		this.processedAt = processedAt;
	}

	public void setProcessedBy(String processedBy) {
		this.processedBy = processedBy;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
    
}
