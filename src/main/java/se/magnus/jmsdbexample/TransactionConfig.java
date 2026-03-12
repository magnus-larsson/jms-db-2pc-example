package se.magnus.jmsdbexample;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import com.ibm.mq.jakarta.jms.MQXAConnectionFactory;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.common.CommonConstants;
import jakarta.jms.XAConnectionFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties({DataSourceProperties.class})
public class TransactionConfig {

    @Value("${ibm.mq.queue-manager}")
    private String queueManager;

    @Value("${ibm.mq.channel}")
    private String channel;

    @Value("${ibm.mq.conn-name}")
    private String connName;

    @Value("${ibm.mq.user}")
    private String mqUser;

    @Value("${ibm.mq.password}")
    private String mqPassword;

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
    public AtomikosConnectionFactoryBean connectionFactory() throws Exception {
        MQXAConnectionFactory mqXAConnectionFactory = new MQXAConnectionFactory();
        mqXAConnectionFactory.setHostName(connName.split("\\(")[0]);
        mqXAConnectionFactory.setPort(Integer.parseInt(connName.split("\\(")[1].replace(")", "")));
        mqXAConnectionFactory.setQueueManager(queueManager);
        mqXAConnectionFactory.setChannel(channel);
        mqXAConnectionFactory.setTransportType(CommonConstants.WMQ_CM_CLIENT);

        // Set the connection username - required for authentication with IBM MQ
        mqXAConnectionFactory.setStringProperty(CommonConstants.USERID, mqUser);
        mqXAConnectionFactory.setStringProperty(CommonConstants.PASSWORD, mqPassword);

        AtomikosConnectionFactoryBean bean = new AtomikosConnectionFactoryBean();
        bean.setUniqueResourceName("ibmmq-" + System.currentTimeMillis() % 1000000 + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        bean.setXaConnectionFactory(mqXAConnectionFactory);
        bean.setPoolSize(20);
        return bean;
    }

    @Bean(name = "nonXaConnectionFactory")
    public MQConnectionFactory nonXaConnectionFactory() throws Exception {
        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(connName.split("\\(")[0]);
        cf.setPort(Integer.parseInt(connName.split("\\(")[1].replace(")", "")));
        cf.setQueueManager(queueManager);
        cf.setChannel(channel);
        cf.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        cf.setStringProperty(CommonConstants.USERID, mqUser);
        cf.setStringProperty(CommonConstants.PASSWORD, mqPassword);
        return cf;
    }

    @Bean(name = "nonXaJmsTemplate")
    public org.springframework.jms.core.JmsTemplate nonXaJmsTemplate(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") MQConnectionFactory nonXaConnectionFactory) {
        org.springframework.jms.core.JmsTemplate template = new org.springframework.jms.core.JmsTemplate(nonXaConnectionFactory);
        template.setReceiveTimeout(1000L);
        return template;
    }
}
