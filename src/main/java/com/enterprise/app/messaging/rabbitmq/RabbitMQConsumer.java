package com.enterprise.app.messaging.rabbitmq;

import com.enterprise.app.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleEmailMessage(
            Map<String, Object> payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing email: to={}, subject={}", payload.get("to"), payload.get("subject"));
            // TODO: integrate with mail sender
            processEmail(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process email: {}", e.getMessage(), e);
            boolean requeue = shouldRequeue(e);
            channel.basicNack(deliveryTag, false, requeue);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleSmsMessage(
            Map<String, Object> payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing SMS: to={}", payload.get("to"));
            processSms(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process SMS: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleNotification(
            Map<String, Object> payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing notification: type={}, userId={}", payload.get("type"), payload.get("userId"));
            processNotification(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderEvent(
            Map<String, Object> payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing order event: orderId={}, event={}", payload.get("orderId"), payload.get("event"));
            processOrderEvent(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handlePayment(
            Map<String, Object> payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing payment: orderId={}, amount={}", payload.get("orderId"), payload.get("amount"));
            processPayment(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process payment: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // DLQ listeners
    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void handleEmailDlq(Map<String, Object> payload) {
        log.error("Dead letter email: to={} — storing for retry/alert", payload.get("to"));
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_DLQ)
    public void handlePaymentDlq(Map<String, Object> payload) {
        log.error("Dead letter payment: orderId={} — requires manual intervention", payload.get("orderId"));
    }

    // ---- Private helpers ----
    private void processEmail(Map<String, Object> payload) {
        log.debug("Sending email to={}", payload.get("to"));
    }

    private void processSms(Map<String, Object> payload) {
        log.debug("Sending SMS to={}", payload.get("to"));
    }

    private void processNotification(Map<String, Object> payload) {
        log.debug("Dispatching notification userId={}", payload.get("userId"));
    }

    private void processOrderEvent(Map<String, Object> payload) {
        log.debug("Processing order orderId={}", payload.get("orderId"));
    }

    private void processPayment(Map<String, Object> payload) {
        log.debug("Processing payment orderId={}", payload.get("orderId"));
    }

    private boolean shouldRequeue(Exception e) {
        return !(e instanceof IllegalArgumentException || e instanceof IllegalStateException);
    }
}