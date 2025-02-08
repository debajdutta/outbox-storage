package io.github.djd.outbox.publisher;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.djd.outbox.model.OutboxMessage;

public class KafkaPublisher implements MessagePublisher {
	
	private static final Logger logger = LoggerFactory.getLogger(KafkaPublisher.class);
	
    private final KafkaProducer<String, String> producer;
    
    private final String topic;

    public KafkaPublisher(String brokerUrl, String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", brokerUrl);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
    }

    @Override
    public void publish(OutboxMessage message) {
        producer.send(new ProducerRecord<>(topic, message.getPayload()));
        logger.debug("Published message with ID {}.", message.getId());
    }
    
    public void close() {
        producer.close();
        logger.debug("closed");
    }
}
