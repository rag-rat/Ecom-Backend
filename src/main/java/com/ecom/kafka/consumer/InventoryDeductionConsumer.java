package com.ecom.kafka.consumer;

import com.ecom.inventory.service.InventoryService;
import com.ecom.kafka.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryDeductionConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "payment-success", groupId = "inventory-group")
    public void handlePaymentSuccess(
            ConsumerRecord<String, PaymentSuccessEvent> record,
            Acknowledgment ack) {

        PaymentSuccessEvent event = record.value();

        log.info("Consumed PaymentSuccessEvent for inventory deduction: orderNumber={}",
                event.getOrderNumber());

        try {
            for (PaymentSuccessEvent.PaymentItem item : event.getItems()) {
                inventoryService.deduct(item.getProductId(), item.getQuantity());
            }

            log.info("Stock deducted for all items in order {}", event.getOrderNumber());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to deduct stock for order {}: {}",
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }
}
