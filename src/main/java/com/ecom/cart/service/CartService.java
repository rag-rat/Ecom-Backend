package com.ecom.cart.service;

import com.ecom.cart.dto.AddToCartRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.entity.Cart;
import com.ecom.cart.mapper.CartMapper;
import com.ecom.cart.repository.CartRepository;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.inventory.repository.InventoryRepository;
import com.ecom.product.entity.Product;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;



    // addToCart()

    public CartResponse addToCart(String userId, AddToCartRequest request) {


        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));


        int availableStock = inventoryRepository.findByProductId(product.getId())
                .map(inv -> inv.getAvailableStock())
                .orElse(0);



        Cart cart = getOrCreateCart(userId);


        Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();


        int requestedTotalQuantity = request.getQuantity() +
                existingItem.map(Cart.CartItem::getQuantity).orElse(0);


        if (requestedTotalQuantity > availableStock) {
            throw new BusinessException(
                    "Only " + availableStock + " unit(s) available in stock");
        }


        if (existingItem.isPresent()) {

            existingItem.get().setQuantity(requestedTotalQuantity);

        } else {

            String primaryImageUrl = product.getImages() != null && !product.getImages().isEmpty()
                    ? product.getImages().stream()
                    .filter(Product.ProductImage::isPrimary)
                    .findFirst()
                    .map(Product.ProductImage::getUrl)
                    .orElse(product.getImages().get(0).getUrl())
                    : null;

            BigDecimal effectivePrice = product.getDiscountedPrice() != null
                    ? product.getDiscountedPrice()
                    : product.getPrice();

            Cart.CartItem newItem = Cart.CartItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(primaryImageUrl)
                    .price(effectivePrice)
                    .quantity(request.getQuantity())
                    .build();

            cart.getItems().add(newItem);
        }

        recalculateTotals(cart);

        Cart saved = cartRepository.save(cart);

        log.info("Product {} added to cart for user {}", request.getProductId(), userId);

        return buildResponseWithStockFlags(saved);
    }



    // updateQuantity()

    public CartResponse updateQuantity(String userId, String productId, int newQuantity) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));


        Cart.CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in cart: " + productId));


        int availableStock = inventoryRepository.findByProductId(productId)
                .map(inv -> inv.getAvailableStock())
                .orElse(0);


        if (newQuantity > availableStock) {
            throw new BusinessException(
                    "Only " + availableStock + " unit(s) available in stock");
        }


        item.setQuantity(newQuantity);
        recalculateTotals(cart);


        Cart saved = cartRepository.save(cart);

        log.info("Cart item quantity updated for user {}: product={}, qty={}",
                userId, productId, newQuantity);

        return buildResponseWithStockFlags(saved);
    }



    // removeItem()

    public CartResponse removeItem(String userId, String productId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        boolean removed = cart.getItems().removeIf(
                item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new ResourceNotFoundException("Product not found in cart: " + productId);
        }

        recalculateTotals(cart);

        Cart saved = cartRepository.save(cart);
        log.info("Product {} removed from cart for user {}", productId, userId);

        return buildResponseWithStockFlags(saved);
    }



    // clearCart()

    public CartResponse clearCart(String userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        cart.setItems(new ArrayList<>());
        recalculateTotals(cart);

        Cart saved = cartRepository.save(cart);
        log.info("Cart cleared for user {}", userId);

        return cartMapper.toResponse(saved);
    }



    // getCart()

    public CartResponse getCart(String userId) {

        Cart cart = cartRepository.findByUserId(userId).orElse(
                Cart.builder().userId(userId).items(new ArrayList<>()).build()
        );

        return buildResponseWithStockFlags(cart);
    }




    // PRIVATE HELPERS


    // getOrCreateCart()

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .totalAmount(BigDecimal.ZERO)
                        .totalItems(0)
                        .build());
    }



    // recalculateTotals()

    private void recalculateTotals(Cart cart) {

        for (Cart.CartItem item : cart.getItems()) {
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal total = cart.getItems().stream()
                .map(Cart.CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = cart.getItems().stream()
                .mapToInt(Cart.CartItem::getQuantity)
                .sum();

        cart.setTotalAmount(total);
        cart.setTotalItems(itemCount);

    }



    // buildResponseWithStockFlags()

    private CartResponse buildResponseWithStockFlags(Cart cart) {

        CartResponse response = cartMapper.toResponse(cart);

        List<CartResponse.CartItemResponse> enrichedItems = response.getItems().stream()
                .map(itemResponse -> {
                    int availableStock = inventoryRepository
                            .findByProductId(itemResponse.getProductId())
                            .map(inv -> inv.getAvailableStock())
                            .orElse(0);

                    itemResponse.setInStock(availableStock >= itemResponse.getQuantity());
                    return itemResponse;
                })
                .toList();

        response.setItems(enrichedItems);
        return response;
    }

}



