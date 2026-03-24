package se.magnus.jmsdbexample;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestReplyListener {

    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyListener.class);

    private final JmsTemplate nonXaJmsTemplate;

    public RequestReplyListener(@Qualifier("nonXaJmsTemplate") JmsTemplate nonXaJmsTemplate) {
        this.nonXaJmsTemplate = nonXaJmsTemplate;
    }

    @JmsListener(destination = "request.queue", containerFactory = "nonXaListenerContainerFactory")
    public void receiveRequest(Message message) throws JMSException {
        String correlationId = message.getJMSCorrelationID();
        Destination replyTo = message.getJMSReplyTo();

        LOG.info("### Received request message with correlationId: {}", correlationId);

        if (!(message instanceof TextMessage textMessage)) {
            LOG.warn("Ignoring non-text request message");
            return;
        }

        String requestBody = textMessage.getText();
        LOG.info("Request body: {}", requestBody);

        if (replyTo == null) {
            LOG.warn("No JMSReplyTo set on request message, cannot send reply");
            return;
        }

        nonXaJmsTemplate.send(replyTo, session -> {
            TextMessage reply = session.createTextMessage(requestBody);
            reply.setJMSCorrelationID(correlationId);
            return reply;
        });

        LOG.info("Sent reply with correlationId: {} to {}", correlationId, replyTo);
    }
}
