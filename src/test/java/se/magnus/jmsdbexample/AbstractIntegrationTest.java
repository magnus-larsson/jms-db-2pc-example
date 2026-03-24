package se.magnus.jmsdbexample;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCommand("postgres", "-c", "max_prepared_transactions=10");

    @Container
    static GenericContainer<?> ibmmq = new GenericContainer<>(DockerImageName.parse("icr.io/ibm-messaging/mq:latest"))
            .withExposedPorts(1414, 9443)
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withEnv("MQ_APP_PASSWORD", "passw0rd")
            .withEnv("MQ_ENABLE_EMBEDDED_WEB_SERVER", "true")
            .waitingFor(Wait.forLogMessage(".*Started web server.*", 1));

    @BeforeAll
    static void setupQueues() throws Exception {
        // Create queues in IBM MQ
        ibmmq.execInContainer("bash", "-c",
            "echo \"DEFINE QLOCAL('inbound.queue') REPLACE\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"DEFINE QLOCAL('DEV.DEAD.LETTER.QUEUE') REPLACE\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"SET AUTHREC PROFILE('inbound.queue') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(ALL)\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"SET AUTHREC PROFILE('DEV.DEAD.LETTER.QUEUE') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(ALL)\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"DEFINE QLOCAL('request.queue') REPLACE\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"DEFINE QLOCAL('reply.queue') REPLACE\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"SET AUTHREC PROFILE('request.queue') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(ALL)\" | runmqsc QM1");
        ibmmq.execInContainer("bash", "-c",
            "echo \"SET AUTHREC PROFILE('reply.queue') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(ALL)\" | runmqsc QM1");
    }

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

        registry.add("ibm.mq.queue-manager", () -> "QM1");
        registry.add("ibm.mq.channel", () -> "DEV.APP.SVRCONN");
        registry.add("ibm.mq.conn-name", () -> ibmmq.getHost() + "(" + ibmmq.getMappedPort(1414) + ")");
        registry.add("ibm.mq.user", () -> "app");
        registry.add("ibm.mq.password", () -> "passw0rd");
    }
}
