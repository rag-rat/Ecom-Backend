package com.ecom.redis;

import com.ecom.product.dto.ProductResponse;
import com.ecom.product.dto.ProductSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final ObjectMapper objectMapper;


    @Value("${app.redis.ttl.product}")
    private long productTtlSeconds;

    @Value("${app.redis.ttl.popular-products}")
    private long popularProductsTtlSeconds;


    private static final String PRODUCT_KEY_PREFIX = "product:";
    private static final String POPULAR_PRODUCTS_KEY = "products:popular";



    // getProduct()

    public ProductResponse getProduct(String productId) {

        String key = PRODUCT_KEY_PREFIX + productId;

        try {

            Object cached = redisObjectTemplate.opsForValue().get(key);

            if (cached == null) {
                log.debug("Cache MISS for product: {}", productId);
                return null;
            }

            log.debug("Cache HIT for product: {}", productId);

            return objectMapper.convertValue(cached, ProductResponse.class);

        } catch (Exception e) {
            log.warn("Failed to read product cache for {}: {}", productId, e.getMessage());
            return null; // treat any cache read failure as a miss, never fail the request
        }
    }



    // cacheProduct()

    public void cacheProduct(String productId, ProductResponse product) {

        String key = PRODUCT_KEY_PREFIX + productId;

        try {

            redisObjectTemplate.opsForValue().set(
                    key, product, productTtlSeconds, TimeUnit.SECONDS);
            log.debug("Cached product: {} (TTL={}s)", productId, productTtlSeconds);

        } catch (Exception e) {

            log.warn("Failed to cache product {}: {}", productId, e.getMessage());
        }
    }



    // evictProduct()

    public void evictProduct(String productId) {

        String key = PRODUCT_KEY_PREFIX + productId;

        try {

            redisObjectTemplate.delete(key);
            log.debug("Evicted product cache: {}", productId);

        } catch (Exception e) {
            log.warn("Failed to evict product cache for {}: {}", productId, e.getMessage());
        }
    }



    // getPopularProducts()

    @SuppressWarnings("unchecked")
    public List<ProductSummaryResponse> getPopularProducts() {

        try {

            Object cached = redisObjectTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);

            if (cached == null) {
                log.debug("Cache MISS for popular products");
                return null;
            }

            log.debug("Cache HIT for popular products");

            return objectMapper.convertValue(
                    cached,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, ProductSummaryResponse.class)
            );

        } catch (Exception e) {
            log.warn("Failed to read popular products cache: {}", e.getMessage());
            return null;
        }
    }



    // cachePopularProducts()

    public void cachePopularProducts(List<ProductSummaryResponse> products) {

        try {

            redisObjectTemplate.opsForValue().set(
                    POPULAR_PRODUCTS_KEY, products, popularProductsTtlSeconds, TimeUnit.SECONDS);
            log.debug("Cached popular products list (TTL={}s)", popularProductsTtlSeconds);

        } catch (Exception e) {
            log.warn("Failed to cache popular products: {}", e.getMessage());
        }
    }



    // evictPopularProducts()

    public void evictPopularProducts() {
        try {
            redisObjectTemplate.delete(POPULAR_PRODUCTS_KEY);
            log.debug("Evicted popular products cache");
        } catch (Exception e) {
            log.warn("Failed to evict popular products cache: {}", e.getMessage());
        }
    }


}
