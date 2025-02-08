The Outbox Storage library provides reliable message persistence for at-least-once delivery in microservices. 
It currently supports MongoDB and uses Polling and CDC mechanisms to process new messages. 
The library also supports publishing data to Azure Service Bus, with plans for more integrations in the future.

Library Structure:

 - io.github.djd.outbox.model.OutboxMessage – Represents a message in the Outbox.
 - io.github.djd.outbox.storage.OutboxStorage – Interface for storing and retrieving Outbox messages.
 - io.github.djd.outbox.storage.MongoOutboxStorage – Implements OutboxStorage using MongoDB.
 - io.github.djd.outbox.publisher.MessagePublisher – Interface for publishing messages (to ServiceBus, Kafka, etc.).
 - io.github.djd.outbox.publisher.ServiceBusPublisher – Publishes messages to Azure Service Bus.
 - io.github.djd.outbox.processor.MessageProcessor – Processes Outbox messages.
 - io.github.djd.outbox.processor.PollingProcessor – Polls the database for unprocessed messages.
 - io.github.djd.outbox.processor.CDCProcessor – Processes CDC (change data capture) events by listening to database change streams.
 - io.github.djd.outbox.service.OutboxService – Exposes API to store messages and starts the processor automatically.

Message Processing Status Flow:

	1. Fetch & Lock (PENDING → IN_PROGRESS)
	2. Process & Publish
	3. Mark Success (IN_PROGRESS → COMPLETED)
	4. On Failure, Reset (COMPLETED → FAILED)

References:
Change data capture
 - https://www.mongodb.com/docs/kafka-connector/current/sink-connector/fundamentals/change-data-capture/

Atomicity in clustered environment.
 - Reference: https://www.mongodb.com/docs/manual/core/write-operations-atomicity/

TODO-
 - Add outbox support to PostgreSQL
 - Add a Kafka message publisher.
 - TestCases
