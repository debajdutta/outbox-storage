package io.github.djd.outbox.publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import io.github.djd.outbox.exception.MessagePublishingException;
import io.github.djd.outbox.model.OutboxMessage;

public class ServiceBusPublisher implements MessagePublisher {
    
	private static final Logger logger = LoggerFactory.getLogger(ServiceBusPublisher.class);
	
	private final ServiceBusClientBuilder clientBuilder;

//    public ServiceBusPublisher(String connectionString) {
//        this.clientBuilder = new ServiceBusClientBuilder()
//                .connectionString(connectionString);
//    }
    
    public ServiceBusPublisher(ServiceBusClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public void publish(OutboxMessage message) throws MessagePublishingException {
    	ServiceBusSenderClient senderClient = null;
    	try {
    		// The message destination topic-name is in the message
    		// Create a sender for the topic
            senderClient = clientBuilder.sender()
                    .topicName(message.getTopic())
                    .buildClient();
    		
    		ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message.getPayload());
            serviceBusMessage.setSubject(message.getTopic());
            senderClient.sendMessage(serviceBusMessage);
            logger.info("Published message with ID {}.", message.getId());
    	} catch(Exception e) {
    		throw new MessagePublishingException("Failed to publish message with ID " + message.getId() + ". ", e);
    	}
        finally {
        	if(senderClient!=null) {
        		senderClient.close();
        		logger.debug("Servicebus client closed.");
        	}
        }
    }

    public void close() {
        logger.debug("closed");
    }
}
