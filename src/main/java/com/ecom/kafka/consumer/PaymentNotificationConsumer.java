package com.ecom.kafka.consumer;

import com.ecom.kafka.event.PaymentSuccessEvent;
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
public class PaymentNotificationConsumer {

    private final NotificationService notificationService;


    @KafkaListener(topics = "payment-success", groupId = "receipt-group")
    public void handlePaymentSuccess(
            ConsumerRecord<String, PaymentSuccessEvent> record,
            Acknowledgment ack) {

        PaymentSuccessEvent event = record.value();

        log.info("Consumed PaymentSuccessEvent for receipt: orderNumber={}, customer={}",
                event.getOrderNumber(), event.getCustomerEmail());

        try {

            String subject = "Payment Receipt - " + event.getOrderNumber();

            String body = "Hi "
                    + event.getCustomerName()
                    + ",\n\n"
                    + "We've received your payment of ₹" + event.getAmountPaid()
                    + " for order "
                    + event.getOrderNumber()
                    + ".\n\n"
                    + "Your order is now confirmed and being prepared for shipment.\n\n"
                    + "Thank you for shopping with us!";



            notificationService.send(event.getCustomerEmail(), subject, body);

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to send receipt for order {}: {}",
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }
}

