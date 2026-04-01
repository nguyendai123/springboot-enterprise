package com.enterprise.app.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableJms
public class ActiveMQConfig {

    public static final String ORDER_QUEUE = "ORDER.QUEUE";
    public static final String PAYMENT_QUEUE = "PAYMENT.QUEUE";
    public static final String INVENTORY_QUEUE = "INVENTORY.QUEUE";
    public static final String BROADCAST_TOPIC = "BROADCAST.TOPIC";

    @Value("${spring.activemq.broker-url:tcp://localhost:61616}")
    private String brokerUrl;

    @Value("${spring.activemq.user:admin}")
    private String username;

    @Value("${spring.activemq.password:admin}")
    private String password;

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        factory.setTrustedPackages(List.of("com.enterprise.app"));
        factory.setUseAsyncSend(true);
        factory.setProducerWindowSize(1024000);
        factory.setOptimizeAcknowledge(true);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    // ==================== JMS TEMPLATES ====================
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(jacksonJmsMessageConverter());
        template.setSessionTransacted(true);
        template.setDeliveryPersistent(true);
        template.setExplicitQosEnabled(true);
        template.setTimeToLive(86400000L); // 24h
        return template;
    }

    @Bean(name = "topicJmsTemplate")
    public JmsTemplate topicJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(jacksonJmsMessageConverter());
        template.setPubSubDomain(true); // for topics
        return template;
    }

    // ==================== LISTENER FACTORIES ====================
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter());
        factory.setSessionTransacted(true);
        factory.setConcurrency("3-10");
        factory.setErrorHandler(t -> log.error("ActiveMQ Listener error: {}", t.getMessage(), t));
        return factory;
    }

    @Bean(name = "topicListenerFactory")
    public DefaultJmsListenerContainerFactory topicListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter());
        factory.setPubSubDomain(true);
        factory.setSubscriptionDurable(true);
        factory.setClientId("enterprise-client");
        return factory;
    }

    // ==================== DESTINATIONS ====================
    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue(ORDER_QUEUE);
    }

    @Bean
    public Queue paymentQueue() {
        return new ActiveMQQueue(PAYMENT_QUEUE);
    }

    @Bean
    public Queue inventoryQueue() {
        return new ActiveMQQueue(INVENTORY_QUEUE);
    }

    @Bean
    public Topic broadcastTopic() {
        return new ActiveMQTopic(BROADCAST_TOPIC);
    }
}