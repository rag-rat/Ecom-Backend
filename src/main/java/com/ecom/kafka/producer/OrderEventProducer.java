package com.ecom.kafka.producer;

import com.ecom.kafka.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "order-created";


    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent for order {}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published OrderCreatedEvent for order {} to partition {}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}