package com.enterprise.app.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    @Value("${app.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Value("${app.kafka.topics.audit-events:audit-events}")
    private String auditEventsTopic;

    public CompletableFuture<SendResult<String, Object>> publishUserEvent(String key, Object event) {
        return publish(userEventsTopic, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishOrderEvent(String key, Object event) {
        return publish(orderEventsTopic, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishNotificationEvent(String key, Object event) {
        return publish(notificationEventsTopic, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishAuditEvent(String key, Object event) {
        return publish(auditEventsTopic, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publish(String topic, String key, Object payload) {
        log.debug("Publishing event to topic={}, key={}", topic, key);
        return kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic={}, key={}: {}", topic, key, ex.getMessage());
                    } else {
                        log.debug("Event published to topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}