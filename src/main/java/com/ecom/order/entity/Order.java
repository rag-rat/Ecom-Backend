package com.ecom.order.entity;

import com.ecom.address.entity.Address;
import com.ecom.common.entity.BaseEntity;
import com.ecom.common.enums.OrderStatus;
import com.ecom.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Document(collection = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Indexed(unique = true)
    @Field("order_number")
    private String orderNumber;

    @Indexed
    @Field("user_id")
    private String userId;

    @Field("items")
    private List<OrderItem> items;


    // * Embedded copy of the address at order time

    @Field("shipping_address")
    private Address shippingAddress;


    @Indexed
    @Builder.Default
    @Field("status")
    private OrderStatus status = OrderStatus.PENDING;


    @Field("subtotal")
    private BigDecimal subtotal;

    @Builder.Default
    @Field("shipping_charge")
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Builder.Default
    @Field("discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @Field("total_amount")
    private BigDecimal totalAmount;


    /*
     * Linked to the Payment document, once payment is initiated.
     * Null while the order is freshly created and awaiting payment.
     */
    @Field("payment_id")
    private String paymentId;

    @Builder.Default
    @Field("payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;



    // Populated only if status is CANCELLED
    @Field("cancel_reason")
    private String cancelReason;

    @Field("cancelled_at")
    private LocalDateTime cancelledAt;


    // Free-text notes — delivery instructions from customer, or
    // internal admin notes (kept simple as one field for this build)
    @Field("notes")
    private String notes;


    @Field("shipped_at")
    private LocalDateTime shippedAt;

    @Field("delivered_at")
    private LocalDateTime deliveredAt;


    /*
     * OrderItem — embedded, permanent snapshot. Notice this does NOT
     * reuse Cart.CartItem (Part 12) even though the shape looks similar —
     * Cart's snapshot is meant to be temporary/mutable browsing
     * convenience; Order's snapshot is permanent financial record.
     * Keeping them as separate types makes that distinction explicit
     * in the codebase, even at the cost of some field duplication.
     */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
