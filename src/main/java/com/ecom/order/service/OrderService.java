package com.ecom.order.service;

import com.ecom.address.entity.Address;
import com.ecom.address.repository.AddressRepository;
import com.ecom.cart.entity.Cart;
import com.ecom.cart.repository.CartRepository;
import com.ecom.common.dto.PageResponse;
import com.ecom.common.enums.OrderStatus;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.inventory.service.InventoryService;
import com.ecom.kafka.event.OrderCreatedEvent;
import com.ecom.kafka.producer.OrderEventProducer;
import com.ecom.order.dto.CancelOrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.PlaceOrderRequest;
import com.ecom.order.entity.Order;
import com.ecom.order.mapper.OrderMapper;
import com.ecom.order.repository.OrderRepository;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;
    private final UserRepository userRepository;


    // Hardcoded flat shipping charge for this build — a real system
    // might calculate this based on address/weight, left as a future enhancement

    private static final BigDecimal SHIPPING_CHARGE = new BigDecimal("99.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000.00");



    // Maps each status to the set of statuses it's legally allowed to move to..

    private static final Set<OrderStatus> FROM_PENDING = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> FROM_CONFIRMED = Set.of(
            OrderStatus.SHIPPED, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> FROM_SHIPPED = Set.of(
            OrderStatus.DELIVERED);

    // DELIVERED and CANCELLED have no further legal transitions — terminal states



    // placeOrder()

    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest request) {

        log.info("Placing order for user {}", userId);


        // 1. Load cart → user must have items.
        // If empty → cannot order.

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Your cart is empty"));


        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty");
        }


        // Validate address → check if this address belongs to that user.
        // Security check.

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: " + request.getAddressId()));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("You can only ship to your own saved addresses");
        }



        /*
         * RESERVE STOCK FOR EVERY ITEM.
         * If ANY single item fails (insufficient stock), inventoryService.reserve()
         * throws BusinessException immediately — @Transactional then rolls
         * back any reservations already made for EARLIER items in this loop.
         * This is precisely why @Transactional is mandatory here: without it,
         * item 1 and 2's stock would stay reserved forever even though the
         * order as a whole failed on item 3.
         */

        List<Order.OrderItem> orderItems = new ArrayList<>();

        // Reserve stock

        for (Cart.CartItem cartItem : cart.getItems()) {
            inventoryService.reserve(cartItem.getProductId(), cartItem.getQuantity());

            orderItems.add(Order.OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .productImage(cartItem.getProductImage())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getSubtotal())
                    .build());
        }


        // Calculate money --> subtotal, shipping, total

        BigDecimal subtotal = orderItems.stream()
                .map(Order.OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingCharge = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_CHARGE;

        BigDecimal totalAmount = subtotal.add(shippingCharge);


        // Generate order 

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .items(orderItems)
                .shippingAddress(address) // embedded snapshot — see entity comment
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingCharge(shippingCharge)
                .discount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .notes(request.getNotes())
                .build();


        // Save Order

        Order saved = orderRepository.save(order);
        log.info("Order {} placed for user {}: total={}", saved.getOrderNumber(), userId, totalAmount);

        // Cart has now been "consumed" into this order — clear it.
        // Reuses CartService's pattern but inlined here directly via
        // repository since CartService.clearCart() would re-fetch the
        // same cart we already have in hand.

        // Clear Cart

        cart.setItems(new ArrayList<>());
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setTotalItems(0);
        cartRepository.save(cart);


        User customer = userRepository.findById(userId).orElse(null);

        if(customer != null) {

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(saved.getId())
                    .orderNumber(saved.getOrderNumber())
                    .userId(userId)
                    .customerEmail(customer.getEmail())
                    .customerName(customer.getFirstName() + " " + customer.getLastName())
                    .items(saved.getItems().stream()
                            .map(item -> OrderCreatedEvent.OrderItemSummary.builder()
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList())
                    .totalAmount(saved.getTotalAmount())
                    .createdAt(saved.getCreatedAt())
                    .build();

            orderEventProducer.publishOrderCreated(event);

        }

        return orderMapper.toResponse(saved);
    }



    // cancelOrder()

    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId, CancelOrderRequest request) {

        Order order = getOwnedOrderOrThrow(userId, orderId);

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(
                    "Order cannot be cancelled once it has been shipped");
        }

        for (Order.OrderItem item : order.getItems()) {
            inventoryService.release(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getReason());
        order.setCancelledAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        log.info("Order {} cancelled by user {}: reason={}",
                order.getOrderNumber(), userId, request.getReason());

        return orderMapper.toResponse(updated);
    }



    // updateOrderStatus()

    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));


        // State machine validation
        // It checks if transition is legal.

        Set<OrderStatus> allowedNext = switch (order.getStatus()) {
            case PENDING -> FROM_PENDING;
            case CONFIRMED -> FROM_CONFIRMED;
            case SHIPPED -> FROM_SHIPPED;
            case DELIVERED, CANCELLED -> Set.of(); // terminal states, no transitions allowed
        };

        if (!allowedNext.contains(newStatus)) {
            throw new BusinessException(
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());

            // Finalize stock: convert reserved stock into sold stock
            for (Order.OrderItem item : order.getItems()) {
                inventoryService.deduct(
                        item.getProductId(),
                        item.getQuantity()
                );
            }
        }


        Order updated = orderRepository.save(order);
        log.info("Order {} status updated to {} by admin", order.getOrderNumber(), newStatus);

        return orderMapper.toResponse(updated);
    }



    // getMyOrders()

    public PageResponse<OrderResponse> getMyOrders(String userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        return buildPageResponse(orderPage);
    }



    // getOrderById()

    public OrderResponse getOrderById(String userId, String orderId) {

        Order order = getOwnedOrderOrThrow(userId, orderId);
        return orderMapper.toResponse(order);

    }



    // getAllOrdersForAdmin()

    public PageResponse<OrderResponse> getAllOrdersForAdmin(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        return buildPageResponse(orderPage);

    }



    // getOrdersByStatusForAdmin()

    public PageResponse<OrderResponse> getOrdersByStatusForAdmin(
            OrderStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByStatus(status, pageable);

        return buildPageResponse(orderPage);
    }



    // getOrderByIdForAdmin()

    public OrderResponse getOrderByIdForAdmin(String orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return orderMapper.toResponse(order);
    }



    // PRIVATE HELPERS


    // getOwnedOrderOrThrow()

    private Order getOwnedOrderOrThrow(String userId, String orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You can only access your own orders");
        }

        return order;
    }


    // generateOrderNumber()

    private String generateOrderNumber() {

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 10000);

        // Production solution --> use an atomic counter

        return "ORD-" + datePart + "-" + randomSuffix;
    }


    // buildPageResponse()

    private PageResponse<OrderResponse> buildPageResponse(Page<Order> orderPage) {

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();

        return PageResponse.of(
                responses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

}

