package com.enterprise.app.messaging.activemq;

import com.enterprise.app.config.ActiveMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

// =========================================================
//  PRODUCER
// =========================================================
@Slf4j
@Service("activeMQProducer")
@RequiredArgsConstructor
class ActiveMQProducer {

    private final JmsTemplate jmsTemplate;

    @Qualifier("topicJmsTemplate")
    private final JmsTemplate topicJmsTemplate;

    public void sendToOrderQueue(Map<String, Object> payload) {
        log.info("ActiveMQ → ORDER.QUEUE orderId={}", payload.get("orderId"));
        jmsTemplate.convertAndSend(ActiveMQConfig.ORDER_QUEUE, payload);
    }

    public void sendToPaymentQueue(Map<String, Object> payload) {
        log.info("ActiveMQ → PAYMENT.QUEUE orderId={}", payload.get("orderId"));
        jmsTemplate.convertAndSend(ActiveMQConfig.PAYMENT_QUEUE, payload);
    }

    public void sendToInventoryQueue(Map<String, Object> payload) {
        log.info("ActiveMQ → INVENTORY.QUEUE productId={}", payload.get("productId"));
        jmsTemplate.convertAndSend(ActiveMQConfig.INVENTORY_QUEUE, payload);
    }

    public void broadcastToTopic(Map<String, Object> payload) {
        log.info("ActiveMQ → BROADCAST.TOPIC type={}", payload.get("type"));
        topicJmsTemplate.convertAndSend(ActiveMQConfig.BROADCAST_TOPIC, payload);
    }
}

// =========================================================
//  CONSUMER
// =========================================================
@Slf4j
@Service("activeMQConsumer")
class ActiveMQConsumer {

    @JmsListener(destination = ActiveMQConfig.ORDER_QUEUE,
                 containerFactory = "jmsListenerContainerFactory")
    public void onOrderMessage(Map<String, Object> payload) {
        try {
            log.info("ActiveMQ ← ORDER.QUEUE orderId={}", payload.get("orderId"));
            String event = (String) payload.getOrDefault("event", "UNKNOWN");
            switch (event) {
                case "ORDER_PLACED"    -> handleOrderPlaced(payload);
                case "ORDER_CONFIRMED" -> handleOrderConfirmed(payload);
                case "ORDER_CANCELLED" -> handleOrderCancelled(payload);
                default -> log.warn("Unknown order event: {}", event);
            }
        } catch (Exception e) {
            log.error("ActiveMQ order processing error: {}", e.getMessage(), e);
            throw e; // triggers JMS retry / DLQ
        }
    }

    @JmsListener(destination = ActiveMQConfig.PAYMENT_QUEUE,
                 containerFactory = "jmsListenerContainerFactory")
    public void onPaymentMessage(Map<String, Object> payload) {
        try {
            log.info("ActiveMQ ← PAYMENT.QUEUE orderId={} amount={}", payload.get("orderId"), payload.get("amount"));
            processPayment(payload);
        } catch (Exception e) {
            log.error("ActiveMQ payment processing error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @JmsListener(destination = ActiveMQConfig.INVENTORY_QUEUE,
                 containerFactory = "jmsListenerContainerFactory")
    public void onInventoryMessage(Map<String, Object> payload) {
        log.info("ActiveMQ ← INVENTORY.QUEUE productId={} qty={}", payload.get("productId"), payload.get("quantity"));
        updateInventory(payload);
    }

    @JmsListener(destination = ActiveMQConfig.BROADCAST_TOPIC,
                 containerFactory = "topicListenerFactory",
                 subscription = "enterprise-broadcast-sub")
    public void onBroadcast(Map<String, Object> payload) {
        log.info("ActiveMQ ← BROADCAST.TOPIC type={}", payload.get("type"));
        handleBroadcast(payload);
    }

    private void handleOrderPlaced(Map<String, Object> p)    { log.debug("Order placed: {}", p.get("orderId")); }
    private void handleOrderConfirmed(Map<String, Object> p)  { log.debug("Order confirmed: {}", p.get("orderId")); }
    private void handleOrderCancelled(Map<String, Object> p)  { log.debug("Order cancelled: {}", p.get("orderId")); }
    private void processPayment(Map<String, Object> p)        { log.debug("Payment processed orderId={}", p.get("orderId")); }
    private void updateInventory(Map<String, Object> p)       { log.debug("Inventory updated productId={}", p.get("productId")); }
    private void handleBroadcast(Map<String, Object> p)       { log.debug("Broadcast received type={}", p.get("type")); }
}