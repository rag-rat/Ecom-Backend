package com.ecom.kafka.consumer;

import com.ecom.kafka.event.OrderCreatedEvent;
import com.ecom.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-created", groupId = "notification-group")
    public void handleOrderCreated(
            ConsumerRecord<String, OrderCreatedEvent> record,
            Acknowledgment ack) {

        OrderCreatedEvent event = record.value();

        log.info("Consumed OrderCreatedEvent: orderNumber={}, customer={}, total={}",
                event.getOrderNumber(), event.getCustomerEmail(), event.getTotalAmount());

        try {

            String subject = "Order Confirmed - " + event.getOrderNumber();

            StringBuilder body = new StringBuilder();

            body
                    .append("Hi ")
                    .append(event.getCustomerName())
                    .append(",\n\n")
                    .append("Thank you for your order! Here's a summary:\n\n")
                    .append("Order Number: ").append(event.getOrderNumber()).append("\n");


            for (var item : event.getItems()) {
                body
                        .append("  - ")
                        .append(item.getProductName())
                        .append(" x").append(item.getQuantity())
                        .append("\n");
            }


            body
                    .append("\nTotal: ₹")
                    .append(event.getTotalAmount())
                    .append("\n\nWe'll notify you once your payment is confirmed.");


            notificationService.send(event.getCustomerEmail(), subject, body.toString());

            // Only acknowledge AFTER processing succeeds
            ack.acknowledge();

        } catch (Exception e) {

            log.error("Failed to process OrderCreatedEvent for order {}: {}", event.getOrderNumber(), e.getMessage(), e);
        }
    }
}
