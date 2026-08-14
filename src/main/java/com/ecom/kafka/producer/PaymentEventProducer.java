package com.ecom.kafka.producer;

import com.ecom.kafka.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "payment-success";

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentSuccessEvent for order {}: {}",
                                event.getOrderId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published PaymentSuccessEvent for order {} to partition {}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

}

