package se.magnus.jmsdbexample;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    private final ProcessedMessageRepository repository;
    private final JmsTemplate nonXaJmsTemplate;

    public MessageConsumer(ProcessedMessageRepository repository,
                           @Qualifier("nonXaJmsTemplate") JmsTemplate nonXaJmsTemplate) {
        this.repository = repository;
        this.nonXaJmsTemplate = nonXaJmsTemplate;
    }

    @JmsListener(destination = "inbound.queue")
    @Transactional
    @Retryable(include = RuntimeException.class, maxAttempts = 4)
    public void receiveMessage(Message message) throws JMSException {
        String messageId = message.getJMSMessageID();
        LOG.info("### Received message with ID: {}", messageId);

        if (message instanceof TextMessage textMessage) {
            String content = textMessage.getText();
            LOG.info("Content: {}", content);

            if ("fail".equals(content)) {
                int deliveryCount = 1;
                try {
                    deliveryCount = message.getIntProperty("JMSXDeliveryCount");
                } catch (JMSException ex) {
                    LOG.warn("Could not read JMSXDeliveryCount property, assuming first delivery", ex);
                }
                LOG.error("Simulating failure for message: {} (attempt #{})", messageId, deliveryCount);
                throw new RuntimeException("Simulated failure");
            }

            ProcessedMessage processedMessage = new ProcessedMessage(messageId, content);
            repository.save(processedMessage);
            LOG.info("Saved message to database");
        }
    }

    @Recover
    public void recover(RuntimeException ex, Message message) throws JMSException {
        if (message instanceof TextMessage textMessage) {
            String content = textMessage.getText();
            String messageId = message.getJMSMessageID();
            LOG.warn("Recovering after retries, sending to DLQ. Message ID: {}", messageId);
            nonXaJmsTemplate.convertAndSend("DEV.DEAD.LETTER.QUEUE", content);
        }
    }
}
