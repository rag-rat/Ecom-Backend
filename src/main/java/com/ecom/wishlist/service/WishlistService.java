package com.ecom.wishlist.service;

import com.ecom.exception.ResourceNotFoundException;
import com.ecom.product.entity.Product;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import com.ecom.wishlist.dto.WishlistResponse;
import com.ecom.wishlist.entity.Wishlist;
import com.ecom.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;


    // addProduct()

    public WishlistResponse addProduct(String userId, String productId) {

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));


        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> Wishlist.builder()
                        .userId(userId)
                        .productIds(new ArrayList<>())
                        .build());

        if (!wishlist.getProductIds().contains(product.getId())) {
            wishlist.getProductIds().add(product.getId());
            wishlistRepository.save(wishlist);
            log.info("Product {} added to wishlist for user {}", productId, userId);
        }

        return buildEnrichedResponse(wishlist);
    }



    // removeProduct()

    public WishlistResponse removeProduct(String userId, String productId) {

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist is empty"));

        wishlist.getProductIds().remove(productId);

        wishlistRepository.save(wishlist);
        log.info("Product {} removed from wishlist for user {}", productId, userId);

        return buildEnrichedResponse(wishlist);
    }



    // getWishlist()

    public WishlistResponse getWishlist(String userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElse(Wishlist.builder().userId(userId).productIds(new ArrayList<>()).build());

        return buildEnrichedResponse(wishlist);
    }



    // buildEnrichedResponse()

    private WishlistResponse buildEnrichedResponse(Wishlist wishlist) {
        List<com.ecom.product.dto.ProductSummaryResponse> products =
                wishlist.getProductIds().stream()
                        .map(productRepository::findByIdAndActiveTrue)
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .map(productMapper::toSummaryResponse)
                        .toList();

        return WishlistResponse.builder()
                .userId(wishlist.getUserId())
                .products(products)
                .totalItems(products.size())
                .build();
    }
}
