package com.enterprise.app.messaging.rabbitmq;

import com.enterprise.app.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendEmail(Map<String, Object> emailPayload) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Sending email message, correlationId={}", correlationId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailPayload,
                msg -> {
                    msg.getMessageProperties().setCorrelationId(correlationId);
                    msg.getMessageProperties().setContentType("application/json");
                    return msg;
                }
        );
    }

    public void sendSms(Map<String, Object> smsPayload) {
        log.info("Sending SMS message to={}", smsPayload.get("to"));
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                RabbitMQConfig.SMS_ROUTING_KEY,
                smsPayload
        );
    }

    public void broadcast(Map<String, Object> payload) {
        log.info("Broadcasting fanout message type={}", payload.get("type"));
        rabbitTemplate.convertAndSend(RabbitMQConfig.FANOUT_EXCHANGE, "", payload);
    }

    public void sendOrderEvent(String routingKey, Map<String, Object> payload) {
        log.info("Sending order event routingKey={}", routingKey);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                routingKey,
                payload
        );
    }

    public void sendPayment(Map<String, Object> paymentPayload) {
        log.info("Sending payment message orderId={}", paymentPayload.get("orderId"));
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                RabbitMQConfig.PAYMENT_ROUTING_KEY,
                paymentPayload
        );
    }
}