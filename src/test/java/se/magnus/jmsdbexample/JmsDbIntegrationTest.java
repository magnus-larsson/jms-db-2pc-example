package se.magnus.jmsdbexample;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@org.springframework.test.annotation.DirtiesContext
public class JmsDbIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("nonXaJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private ProcessedMessageRepository repository;

    @Test
    public void testSuccessfulProcessing() {
        String content = "Hello JMS";
        long before = repository.count();
        jmsTemplate.convertAndSend("inbound.queue", content);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(repository.count()).isEqualTo(before + 1);
            assertThat(repository.findAll().stream().anyMatch(pm -> content.equals(pm.getContent()))).isTrue();
        });

        // Verify no message left in inbound.queue
        Message message = jmsTemplate.receive("inbound.queue");
        assertThat(message).isNull();
    }

    @Test
    public void testFailedProcessingGoesToDlq() {
        String content = "fail";
        long before = repository.count();
        jmsTemplate.convertAndSend("inbound.queue", content);

        // Should try 3 times and then end up in DLQ
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Message dlqMessage = jmsTemplate.receive("DEV.DEAD.LETTER.QUEUE");
            assertThat(dlqMessage).isNotNull();
            assertThat(((TextMessage) dlqMessage).getText()).isEqualTo(content);
        });

        // Verify DB unchanged
        assertThat(repository.count()).isEqualTo(before);

        // Verify no message left in inbound.queue
        Message inboundMessage = jmsTemplate.receive("inbound.queue");
        assertThat(inboundMessage).isNull();
    }
}
