package se.magnus.jmsdbexample;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCommand("postgres", "-c", "max_prepared_transactions=10");

    @Container
    static ActiveMQContainer activemq = new ActiveMQContainer("apache/activemq-classic:5.18.3");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.xa.properties.user", postgres::getUsername);
        registry.add("spring.datasource.xa.properties.password", postgres::getPassword);
        registry.add("spring.datasource.xa.properties.databaseName", postgres::getDatabaseName);
        registry.add("spring.datasource.xa.properties.serverName", postgres::getHost);
        registry.add("spring.datasource.xa.properties.portNumber", () -> postgres.getMappedPort(5432).toString());

        registry.add("spring.activemq.broker-url", () -> activemq.getBrokerUrl() + "?jms.redeliveryPolicy.maximumRedeliveries=2&jms.redeliveryPolicy.initialRedeliveryDelay=100");
    }
}
