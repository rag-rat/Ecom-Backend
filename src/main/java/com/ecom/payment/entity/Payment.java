package com.ecom.payment.entity;

import com.ecom.common.entity.BaseEntity;
import com.ecom.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;


@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Indexed
    @Field("order_id")
    private String orderId;

    @Indexed
    @Field("user_id")
    private String userId;


    /*
     * Razorpay's own order identifier — different from OUR orderId.
     * Razorpay requires creating a "Razorpay order" before the checkout
     * widget can open; this is THEIR tracking ID for the payment session,
     * distinct from our own Order document's orderNumber.
     */
    @Indexed(unique = true)
    @Field("razorpay_order_id")
    private String razorpayOrderId;


    // Populated only after the customer completes payment in the widget
    @Field("razorpay_payment_id")
    private String razorpayPaymentId;


    // Populated only after verification — the HMAC signature Razorpay sent back, which we validate to prove the payment is authentic
    @Field("razorpay_signature")
    private String razorpaySignature;


    @Field("amount")
    private BigDecimal amount;

    @Builder.Default
    @Field("currency")
    private String currency = "INR";

    @Builder.Default
    @Field("status")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Field("method")
    private String method; // e.g. "card", "upi", "netbanking" — from webhook payload

    @Field("failure_reason")
    private String failureReason;

}