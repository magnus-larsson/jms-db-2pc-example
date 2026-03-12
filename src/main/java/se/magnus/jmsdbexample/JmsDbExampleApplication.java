package se.magnus.jmsdbexample;

import jakarta.jms.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableRetry
public class JmsDbExampleApplication implements CommandLineRunner {

	private final JmsTemplate jmsTemplate;

	private final ProcessedMessageRepository repository;

	public JmsDbExampleApplication(@Qualifier("nonXaJmsTemplate") JmsTemplate jmsTemplate, ProcessedMessageRepository repository) {
		this.jmsTemplate = jmsTemplate;
		this.repository = repository;
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(JmsDbExampleApplication.class, args);
		// Application will run CommandLineRunner.run() and then exit
	}

	@Override
	public void run(String... args) {
		System.out.println("### Command line application started");
		System.out.println("### Arguments: " + String.join(", ", args));

		// Add your command line logic here
		// For example, process a specific number of messages then exit
		testSuccessfulProcessing();

		System.out.println("### Command line application completed");
	}

	public void testSuccessfulProcessing() {
		String content = "Hello JMS";
		long before = repository.count();
		jmsTemplate.convertAndSend("inbound.queue", content);

		for (int i = 0; i < 10; i++) {
			if (repository.count() == before + 1 &&
				repository.findAll().stream().anyMatch(pm -> content.equals(pm.getContent()))) {
				break;
			}
			System.out.println("### Waiting for message to be processed...");
      try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
    }
		if (repository.count() != before + 1)
			throw new RuntimeException("Expected repository count to be " + (before + 1) + " but was " + repository.count());
		if (repository.findAll().stream().noneMatch(pm -> content.equals(pm.getContent())))
			throw new RuntimeException("Expected a message with content '" + content + "' in the repository");

		// Verify no message left in inbound.queue
		Message message = jmsTemplate.receive("inbound.queue");
		if (message != null)
			throw new RuntimeException("Expected no message in inbound.queue but found one");
	}
}
