package com.ecom.inventory.service;

import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.inventory.dto.InventoryResponse;
import com.ecom.inventory.entity.Inventory;
import com.ecom.inventory.mapper.InventoryMapper;
import com.ecom.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


    // This service holds the FOUR PRIMITIVE STOCK OPERATIONS that every other module composes:

    // createInventory()  — called by ProductService when admin creates a product
    // reserve()           — called by OrderService when an order is placed (Part 16)
    // release()            — called by OrderService when an order is cancelled (Part 16)
    //deduct()              — called by the Kafka payment-success consumer (Part 19)
    // restock()              — called by AdminInventoryController directly


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {


 private final InventoryRepository inventoryRepository;
 private final InventoryMapper inventoryMapper;


 // createInventory()

 public InventoryResponse createInventory(String productId, int initialStock) {
  if (inventoryRepository.existsByProductId(productId)) {
   throw new BusinessException("Inventory already exists for this product");
  }

  Inventory inventory = Inventory.builder()
          .productId(productId)
          .totalStock(initialStock)
          .reservedStock(0)
          .lowStockThreshold(5)
          .lastRestockedAt(initialStock > 0 ? LocalDateTime.now() : null)
          .build();

  Inventory saved = inventoryRepository.save(inventory);
  log.info("Inventory created for product {}: initial stock {}", productId, initialStock);
  return inventoryMapper.toResponse(saved);
 }


 // reserve()

 public void reserve(String productId, int quantity) {
  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));

  if (inventory.getAvailableStock() < quantity) {
   throw new BusinessException(
           "Insufficient stock. Only " + inventory.getAvailableStock() +
                   " unit(s) available.");
  }

  inventory.setReservedStock(inventory.getReservedStock() + quantity);
  inventoryRepository.save(inventory);
  log.info("Reserved {} unit(s) of product {}. Reserved total now: {}",
          quantity, productId, inventory.getReservedStock());
 }



 // release()

 public void release(String productId, int quantity) {
  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));


  int newReserved = Math.max(0, inventory.getReservedStock() - quantity);
  inventory.setReservedStock(newReserved);
  inventoryRepository.save(inventory);
  log.info("Released {} unit(s) of product {}. Reserved total now: {}",
          quantity, productId, newReserved);
 }



 // deduct()

 public void deduct(String productId, int quantity) {

  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));

  inventory.setTotalStock(Math.max(0, inventory.getTotalStock() - quantity));
  inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - quantity));
  inventoryRepository.save(inventory);
  log.info("Deducted {} unit(s) of product {} after payment confirmation. " +
          "Total stock now: {}", quantity, productId, inventory.getTotalStock());
 }



 // restock()

 public InventoryResponse restock(String productId, int quantity) {
  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));

  inventory.setTotalStock(inventory.getTotalStock() + quantity);
  inventory.setLastRestockedAt(LocalDateTime.now());

  Inventory updated = inventoryRepository.save(inventory);
  log.info("Restocked product {} with {} unit(s). New total: {}",
          productId, quantity, updated.getTotalStock());
  return inventoryMapper.toResponse(updated);
 }


 // updateLowStockThreshold()

 public InventoryResponse updateLowStockThreshold(String productId, int threshold) {
  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));

  inventory.setLowStockThreshold(threshold);
  Inventory updated = inventoryRepository.save(inventory);
  return inventoryMapper.toResponse(updated);
 }



 // getInventoryByProductId()

 public InventoryResponse getInventoryByProductId(String productId) {
  Inventory inventory = inventoryRepository.findByProductId(productId)
          .orElseThrow(() -> new ResourceNotFoundException(
                  "Inventory not found for product: " + productId));
  return inventoryMapper.toResponse(inventory);
 }
}
