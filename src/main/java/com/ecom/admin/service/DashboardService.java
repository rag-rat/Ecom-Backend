package com.ecom.admin.service;


import com.ecom.admin.dto.DashboardSummaryResponse;
import com.ecom.common.enums.OrderStatus;
import com.ecom.inventory.entity.Inventory;
import com.ecom.inventory.repository.InventoryRepository;
import com.ecom.order.entity.Order;
import com.ecom.order.repository.OrderRepository;
import com.ecom.product.entity.Product;
import com.ecom.product.repository.ProductRepository;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


// Current approach = simpler for learning/small scale
// Mongo aggregation = better for large scale


@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;


    /*
     * getDashboardSummary()
     * ────────────────────────
     * Single method assembling the entire admin landing page. Each
     * section below is a self-contained calculation — read them as
     * independent blocks, not a single complex flow.
     */
    public DashboardSummaryResponse getDashboardSummary() {


        List<Order> allOrders = orderRepository.findAll();


        // ── Revenue: sum totalAmount for orders that reached CONFIRMED
        // or beyond (PENDING orders haven't actually been paid for yet,
        // CANCELLED orders generated no revenue) ──
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // ── Orders grouped by status ──
        Map<String, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));


        // Ensure every status appears even with zero count, so the
        // frontend doesn't need to handle missing keys defensively
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.putIfAbsent(status.name(), 0L);
        }


        // ── Low stock alerts: every inventory record where
        // isLowStock() (Part 10's computed method) returns true ──
        List<DashboardSummaryResponse.LowStockAlert> lowStockAlerts =

                inventoryRepository.findAll()
                .stream()
                .filter(Inventory::isLowStock)
                .map(inv -> {
                    String productName = productRepository.findById(inv.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");

                    return DashboardSummaryResponse.LowStockAlert.builder()
                            .productId(inv.getProductId())
                            .productName(productName)
                            .availableStock(inv.getAvailableStock())
                            .threshold(inv.getLowStockThreshold())
                            .build();
                })
                .toList();


        // ── Top selling products: aggregate quantity sold across all
        // CONFIRMED+ orders' embedded items, then sort descending ──
        Map<String, Integer> unitsSoldByProduct = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();

        allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    unitsSoldByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                    productNames.putIfAbsent(item.getProductId(), item.getProductName());
                });



        List<DashboardSummaryResponse.TopProduct> topProducts = unitsSoldByProduct.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> DashboardSummaryResponse.TopProduct.builder()
                        .productId(entry.getKey())
                        .productName(productNames.get(entry.getKey()))
                        .totalUnitsSold(entry.getValue())
                        .build())
                .toList();



        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(allOrders.size())
                .ordersByStatus(ordersByStatus)
                .totalCustomers(userRepository.count())
                .totalProducts(productRepository.count())
                .lowStockProducts(lowStockAlerts)
                .topSellingProducts(topProducts)
                .build();
    }

}
