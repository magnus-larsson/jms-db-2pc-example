package se.magnus.jmsdbexample;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JmsConfig {

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer,
            PlatformTransactionManager transactionManager) {
        
        // Ensure the connection factory is configured for redelivery
        if (connectionFactory instanceof org.springframework.jms.connection.CachingConnectionFactory ccf) {
             // ...
        }

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setTransactionManager(transactionManager);
        factory.setSessionTransacted(true);
        factory.setReceiveTimeout(1000L);
        return factory;
    }
}
