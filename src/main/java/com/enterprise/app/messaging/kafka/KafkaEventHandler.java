package com.enterprise.app.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventHandler {

    @KafkaListener(
            topics = "${app.kafka.topics.user-events:user-events}",
            groupId = "enterprise-user-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        try {
            log.info("Received user event from topic={}, partition={}, offset={}", topic, partition, offset);
            log.debug("Event payload: {}", event);

            String eventType = (String) event.getOrDefault("eventType", "UNKNOWN");
            switch (eventType) {
                case "USER_CREATED" -> handleUserCreated(event);
                case "USER_UPDATED" -> handleUserUpdated(event);
                case "USER_DELETED" -> handleUserDeleted(event);
                case "USER_LOGIN" -> handleUserLogin(event);
                default -> log.warn("Unknown user event type: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
            // Don't ack — will trigger DLT after retries
            throw e;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-events:order-events}",
            groupId = "enterprise-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            ConsumerRecord<String, Map<String, Object>> record,
            Acknowledgment ack) {
        try {
            log.info("Received order event: key={}", record.key());
            Map<String, Object> event = record.value();

            String eventType = (String) event.getOrDefault("eventType", "UNKNOWN");
            switch (eventType) {
                case "ORDER_CREATED" -> handleOrderCreated(event);
                case "ORDER_PAID" -> handleOrderPaid(event);
                case "ORDER_SHIPPED" -> handleOrderShipped(event);
                case "ORDER_CANCELLED" -> handleOrderCancelled(event);
                default -> log.warn("Unknown order event type: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events:notification-events}",
            groupId = "enterprise-notification-group"
    )
    public void handleNotificationEvent(
            @Payload Map<String, Object> event,
            Acknowledgment ack) {
        try {
            log.info("Received notification event: type={}", event.get("type"));
            // Forward to notification service
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification event", e);
            throw e;
        }
    }

    // DLT Listener
    @KafkaListener(topics = "user-events.DLT", groupId = "enterprise-dlt-group")
    public void handleUserEventDlt(ConsumerRecord<String, ?> record, Acknowledgment ack) {
        log.error("Dead Letter: user event failed. key={}, value={}", record.key(), record.value());
        // Store to DB for manual processing / alerting
        ack.acknowledge();
    }

    private void handleUserCreated(Map<String, Object> event) {
        log.info("Processing USER_CREATED: userId={}", event.get("userId"));
        // Send welcome email, init user profile, etc.
    }

    private void handleUserUpdated(Map<String, Object> event) {
        log.info("Processing USER_UPDATED: userId={}", event.get("userId"));
    }

    private void handleUserDeleted(Map<String, Object> event) {
        log.info("Processing USER_DELETED: userId={}", event.get("userId"));
    }

    private void handleUserLogin(Map<String, Object> event) {
        log.info("Processing USER_LOGIN: userId={}", event.get("userId"));
    }

    private void handleOrderCreated(Map<String, Object> event) {
        log.info("Processing ORDER_CREATED: orderId={}", event.get("orderId"));
    }

    private void handleOrderPaid(Map<String, Object> event) {
        log.info("Processing ORDER_PAID: orderId={}", event.get("orderId"));
    }

    private void handleOrderShipped(Map<String, Object> event) {
        log.info("Processing ORDER_SHIPPED: orderId={}", event.get("orderId"));
    }

    private void handleOrderCancelled(Map<String, Object> event) {
        log.info("Processing ORDER_CANCELLED: orderId={}", event.get("orderId"));
    }
}