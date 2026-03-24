# Receive message and store it in a database using 2 PC transactions

The source code in this project has been created using JetBrain's AI assistent Junie.

## AI prompt

Enhance the Spring Boot application to consume messages via JMS using Apache ActiveMQ and store processed records together with the JMS MessageId in a PostgreSQL database using Spring Data JPA. The MessageId shall be used as the Primary Key in the table. For transaction handling, it shall apply the 2 PC protocol. I also need some integration tests using Testcontainers to verify that it works as expected. The tests should also cover negative tests that verify that a message is not consumed if the database update fails, the message should instead be placed on a DLQ after three failed attempts. The integration test for the negative test should verify that the message arrived in the DLQ. All integration tests shall verify that there is no message left on the in-queue after the processing is done, i.e. that they are consumed. I shall be possible to run all tests together in IntelliJ IDEA.

## AI promot tor req/reply test case

Enhance the Spring Boot application with a async request reply example over JMS the provided IBM MQ implementation. The request mesasge shall be a string and the reply mesasge shall contain the value from the request message. Also, provide
an integration test that uses Testcontainers to verify that logic. The test shall specify a correlation id on in reuqest message and verify that the reply message contains this correlation id.

## Result reported by the AI Agent

**Changed Files**
- build.gradle
- TransactionConfig.java
- JmsDbIntegrationTest.java
- JmsConfig.java
- MessageConsumer.java
- JmsDbExampleApplication.java
- test_output.txt

**New Files**
- tmlog.lck
- tmlog2.log
- tmlog4.log

**What I built**
* Added full JMS→DB pipeline with 2PC (JTA) using Atomikos on Spring Boot 3.5.
* Consumed messages from ActiveMQ Classic and persisted ProcessedMessage rows to PostgreSQL with JMSMessageID as the primary key using Spring Data JPA.
* Implemented XA resources for both JMS and JDBC (ActiveMQ XA CF and PG XA DS) and a JtaTransactionManager so message consumption and DB write are in one global transaction.
* Integration tests with Testcontainers for PostgreSQL and ActiveMQ Classic, including negative-path coverage and DLQ verification.

**Key implementation details**

* Dependencies (Gradle):
   * spring-boot-starter-activemq, spring-boot-starter-data-jpa, Atomikos (Jakarta) modules for JTA/XA, postgresql, spring-retry, and Testcontainers (postgresql, activemq, junit-jupiter, Boot TC support) + awaitility.

* JMS consumer:
   * @JmsListener on inbound.queue, @Transactional (JTA), saves a ProcessedMessage entity with messageId (JMS MessageID) as @Id and the payload content.
   * For failures, added Spring Retry (@Retryable(maxAttempts = 3)) to retry processing three times under transaction.
   * On exhausting retries, @Recover publishes the original payload to ActiveMQ.DLQ and completes without persisting the record

* XA/2PC setup:
   * Configured AtomikosDataSourceBean for PostgreSQL XA and AtomikosConnectionFactoryBean wrapping ActiveMQXAConnectionFactory for JMS XA.
   * Exposed a separate non‑XA ActiveMQConnectionFactory and JmsTemplate (bean nonXaJmsTemplate) used by tests and by DLQ publishing in the recover path, leaving the listener on the XA CF for 2PC.
   * The JMS listener container factory is configured to use the JTA transaction manager and transacted sessions.

* Testcontainers integration tests:
   * PostgreSQL started with -c max_prepared_transactions=10 to support XA prepare.
   * ActiveMQ Classic via apache/activemq-classic:5.18.3.
   * Dynamic properties wire Testcontainers URLs into Spring.
   * Positive test: sends a message, asserts it’s persisted and that no message is left on the inbound queue.
   * Negative test: sends a "fail" message; verifies after 3 attempts it is routed to ActiveMQ.DLQ, repository count is unchanged (no persisted row), and the inbound queue is empty.

**How to run in IntelliJ IDEA**
* Open the project, import Gradle, run all tests (Gradle task test or the two tests in JmsDbIntegrationTest).
* Docker must be available locally for Testcontainers.

**Result**

* Both integration tests pass in this environment:
   * testSuccessfulProcessing
   * testFailedProcessingGoesToDlq

* Requirements met:
   * 2PC across JMS and DB using JTA (Atomikos).
   * Persist with JMSMessageID as PK via JPA.
   * DLQ after three failed attempts (verified in tests).
   * No messages left on inbound queue at the end of each test.
   * Tests runnable together in IntelliJ IDEA.