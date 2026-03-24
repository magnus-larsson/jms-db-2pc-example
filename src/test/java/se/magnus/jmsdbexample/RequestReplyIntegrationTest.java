package se.magnus.jmsdbexample;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
public class RequestReplyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("nonXaJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Test
    public void testAsyncRequestReply() {
        String correlationId = "test-correlation-" + UUID.randomUUID();
        String requestBody = "Hello Request Reply";

        jmsTemplate.send("request.queue", session -> {
            TextMessage request = session.createTextMessage(requestBody);
            request.setJMSCorrelationID(correlationId);
            request.setJMSReplyTo(session.createQueue("reply.queue"));
            return request;
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message reply = jmsTemplate.receiveSelected(
                    "reply.queue", "JMSCorrelationID = '" + correlationId + "'");
            assertThat(reply).isNotNull();
            assertThat(reply).isInstanceOf(TextMessage.class);
            assertThat(((TextMessage) reply).getText()).isEqualTo(requestBody);
            assertThat(reply.getJMSCorrelationID()).isEqualTo(correlationId);
        });
    }
}
