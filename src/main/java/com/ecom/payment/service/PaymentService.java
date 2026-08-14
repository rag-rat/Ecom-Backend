package com.ecom.payment.service;

import com.ecom.common.enums.OrderStatus;
import com.ecom.common.enums.PaymentStatus;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.inventory.service.InventoryService;
import com.ecom.kafka.event.PaymentSuccessEvent;
import com.ecom.kafka.producer.PaymentEventProducer;
import com.ecom.order.entity.Order;
import com.ecom.order.repository.OrderRepository;
import com.ecom.payment.dto.CreatePaymentOrderRequest;
import com.ecom.payment.dto.CreatePaymentOrderResponse;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.dto.VerifyPaymentRequest;
import com.ecom.payment.entity.Payment;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final RazorpayClient razorpayClient;
    private final PaymentEventProducer paymentEventProducer;
    private final UserRepository userRepository;


    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;


    // createPaymentOrder()

    @Transactional
    public CreatePaymentOrderResponse createPaymentOrder(String userId, CreatePaymentOrderRequest request) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + request.getOrderId()));


        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You can only pay for your own orders");
        }


        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                    "This order is not awaiting payment (current status: " + order.getStatus() + ")");
        }


        try {

            JSONObject orderRequest = new JSONObject();

            // Razorpay amounts are in the smallest currency unit (paise for INR)
            int amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");


            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .userId(userId)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .status(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);
            log.info("Razorpay order created: {} for our order {}",
                    razorpayOrderId, order.getOrderNumber());


            return CreatePaymentOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .keyId(keyId) // public key — safe to send to frontend
                    .orderId(order.getId())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new BusinessException("Unable to initiate payment. Please try again.");
        }
    }



    // verifyPayment()

    @Transactional
    public PaymentResponse verifyPayment(String userId, VerifyPaymentRequest request) {

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("You can only verify your own payments");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean isValid = verifySignature(payload, request.getRazorpaySignature(), keySecret);


        if (!isValid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);

            log.warn("Payment signature verification FAILED for razorpayOrderId={}",
                    request.getRazorpayOrderId());
            throw new BusinessException("Payment verification failed. Please contact support.");
        }


        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment updated = paymentRepository.save(payment);

        confirmOrderPayment(payment.getOrderId());

        log.info("Payment verified successfully for order {}", payment.getOrderId());

        return toResponse(updated);
    }



    // processWebhook()

    @Transactional
    public void processWebhook(String rawBody, String signatureHeader) {

        boolean isValid = verifySignature(rawBody, signatureHeader, webhookSecret);

        if (!isValid) {
            log.warn("Webhook signature verification FAILED — possible spoofed request");
            throw new BusinessException("Invalid webhook signature");
        }


        JSONObject payload = new JSONObject(rawBody);

        String event = payload.getString("event");
        log.info("Webhook received: event={}", event);


        // We only act on the event that confirms a successful capture —
        // other event types (e.g. payment.failed) are logged but not
        // actively handled in this build; extend here as needed.
        if ("payment.captured".equals(event)) {
            JSONObject paymentEntity = payload
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");


            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");
            String method = paymentEntity.optString("method", null);


            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment record not found for razorpayOrderId: " + razorpayOrderId));



            // IDEMPOTENCY CHECK: if client-side verify() already processed this payment (and confirmOrderPayment already ran), don't process it again when the webhook arrives moments later.
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Payment {} already confirmed — webhook is a duplicate confirmation, skipping",
                        razorpayOrderId);
                return;
            }

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setMethod(method);
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            confirmOrderPayment(payment.getOrderId());
            log.info("Order payment confirmed via webhook for payment {}", razorpayOrderId);
        }
    }



    // confirmOrderPayment()

    private void confirmOrderPayment(String orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));


        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order {} already confirmed — skipping duplicate confirmation", orderId);
            return;
        }


        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is in unexpected status {} during payment confirmation",
                    orderId, order.getStatus());
            return;
        }


        // THE MOMENT STOCK IS GENUINELY DEDUCTED — see Part 10's
        // distinction between reserve() (at order placement) and
        // deduct() (only now, at confirmed payment)
//        for (Order.OrderItem item : order.getItems()) {
//            inventoryService.deduct(item.getProductId(), item.getQuantity());
//        }


        // NEW — publish the event instead of deducting directly

        User customer = userRepository.findById(order.getUserId()).orElse(null);

        if(customer != null){

            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .customerEmail(customer.getEmail())
                    .customerName(customer.getFirstName() + " " + customer.getLastName())
                    .items(order.getItems().stream()
                            .map(item -> PaymentSuccessEvent.PaymentItem.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList())
                    .amountPaid(order.getTotalAmount())
                    .build();

            paymentEventProducer.publishPaymentSuccess(event);
        }


        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order);

        log.info("Order {} confirmed and stock deducted", order.getOrderNumber());


    }



    // verifySignature()

    private boolean verifySignature(String payload, String receivedSignature, String secret) {

        try {

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(secretKeySpec);


            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);

            return computedSignature.equals(receivedSignature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error: {}", e.getMessage(), e);
            return false;
        }
    }



    // toResponse()

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .build();
    }
}



