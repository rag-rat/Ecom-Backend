package com.ecom.order.repository;

import com.ecom.common.enums.OrderStatus;
import com.ecom.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserId(String userId, Pageable pageable);

    /*
     * findByUserIdAndIdAndStatus()
     * Used for the cancel-order flow: we need to confirm the order
     * belongs to this user AND is currently in a cancellable status,
     * all in one query rather than fetching first and checking in Java —
     * though OrderService actually does fetch-then-check for clearer
     * error messages (see service comments). This method exists for
     * potential future use where a combined query is preferable.
     */

    Optional<Order> findByIdAndUserId(String id, String userId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);


    boolean existsByUserIdAndStatusAndItemsProductId(
            String userId,
            OrderStatus status,
            String productId
    );
}