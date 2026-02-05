package se.magnus.jmsdbexample;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, ActiveMQProperties.class})
public class TransactionConfig {

    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager userTransactionManager() throws Exception {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(false);
        return userTransactionManager;
    }

    @Bean
    public UserTransaction userTransaction() throws Exception {
        UserTransactionImp userTransactionImp = new UserTransactionImp();
        userTransactionImp.setTransactionTimeout(300);
        return userTransactionImp;
    }

    @Bean
    public JtaTransactionManager transactionManager(UserTransaction userTransaction, TransactionManager transactionManager) throws Exception {
        return new JtaTransactionManager(userTransaction, transactionManager);
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");
        Properties xaProperties = new Properties();
        xaProperties.setProperty("url", properties.getUrl());
        xaProperties.setProperty("user", properties.getUsername());
        xaProperties.setProperty("password", properties.getPassword());
        ds.setXaProperties(xaProperties);
        ds.setUniqueResourceName("pg-" + System.currentTimeMillis() % 1000000 + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        ds.setPoolSize(20);
        return ds;
    }

    @Bean
    public AtomikosConnectionFactoryBean connectionFactory(ActiveMQProperties properties) {
        ActiveMQXAConnectionFactory xaConnectionFactory = new ActiveMQXAConnectionFactory();
        xaConnectionFactory.setBrokerURL(properties.getBrokerUrl());
        if (properties.getUser() != null) xaConnectionFactory.setUserName(properties.getUser());
        if (properties.getPassword() != null) xaConnectionFactory.setPassword(properties.getPassword());
        // Configure broker-side redelivery max to 2 retries (total 3 deliveries)
        org.apache.activemq.RedeliveryPolicy rp = new org.apache.activemq.RedeliveryPolicy();
        rp.setMaximumRedeliveries(2);
        rp.setInitialRedeliveryDelay(100);
        xaConnectionFactory.setRedeliveryPolicy(rp);

        AtomikosConnectionFactoryBean bean = new AtomikosConnectionFactoryBean();
        bean.setUniqueResourceName("amq-" + System.currentTimeMillis() % 1000000 + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        bean.setXaConnectionFactory(xaConnectionFactory);
        bean.setPoolSize(20);
        return bean;
    }
    @Bean(name = "nonXaConnectionFactory")
    public ActiveMQConnectionFactory nonXaConnectionFactory(ActiveMQProperties properties) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
        cf.setBrokerURL(properties.getBrokerUrl());
        if (properties.getUser() != null) cf.setUserName(properties.getUser());
        if (properties.getPassword() != null) cf.setPassword(properties.getPassword());
        return cf;
    }

    @Bean(name = "nonXaJmsTemplate")
    public org.springframework.jms.core.JmsTemplate nonXaJmsTemplate(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ActiveMQConnectionFactory nonXaConnectionFactory) {
        org.springframework.jms.core.JmsTemplate template = new org.springframework.jms.core.JmsTemplate(nonXaConnectionFactory);
        template.setReceiveTimeout(1000L);
        return template;
    }
}
