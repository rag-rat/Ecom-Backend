package com.ecom.review.service;

import com.ecom.common.dto.PageResponse;
import com.ecom.common.enums.OrderStatus;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.order.repository.OrderRepository;
import com.ecom.product.entity.Product;
import com.ecom.product.repository.ProductRepository;
import com.ecom.redis.ProductCacheService;
import com.ecom.review.dto.CreateReviewRequest;
import com.ecom.review.dto.ProductRatingsSummary;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.dto.UpdateReviewRequest;
import com.ecom.review.entity.Review;
import com.ecom.review.mapper.ReviewMapper;
import com.ecom.review.repository.ReviewRepository;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final OrderRepository orderRepository;
    private final ProductCacheService productCacheService;


    // createReview()

    public ReviewResponse createReview(CreateReviewRequest request, String userId) {

        log.info("Creating review: user={}, product={}", userId, request.getProductId());


        // Product must exist and be active

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));


        // User must not have already reviewed this product

        if (reviewRepository.existsByProductIdAndUserIdAndActiveTrue(
                request.getProductId(), userId)) {
            throw new BusinessException(
                    "You have already reviewed this product. " +
                            "You can edit your existing review instead.");
        }



        // User must exist

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        // Verified purchase flag set

        boolean verified = hasVerifiedPurchase(userId, request.getProductId());


        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .userName(user.getFirstName() + " " + user.getLastName())
                .rating(request.getRating())
                .title(request.getTitle())
                .reviewText(request.getReviewText())
                .verifiedPurchase(verified)
                .active(true)
                .build();


        Review savedReview = reviewRepository.save(review);
        log.info("Review {} created for product {}", savedReview.getId(), request.getProductId());


        // recalculate the product's rating aggregate

        updateProductRatingStats(request.getProductId());


        ReviewResponse response = reviewMapper.toResponse(savedReview);
        response.setOwner(true);
        return response;

    }



    // updateReview()

    public ReviewResponse updateReview(String reviewId, UpdateReviewRequest request, String userId) {

        // review must exist

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));


        // review belong to user

        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("You can only edit your own reviews");
        }


        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getTitle() != null) review.setTitle(request.getTitle());
        if (request.getReviewText() != null) review.setReviewText(request.getReviewText());


        Review updated = reviewRepository.save(review);
        log.info("Review {} updated by user {}", reviewId, userId);

        updateProductRatingStats(review.getProductId());

        ReviewResponse response = reviewMapper.toResponse(updated);
        response.setOwner(true);
        return response;
    }



    // deleteReview()

    public void deleteReview(String reviewId, String userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("You can only delete your own reviews");
        }

        review.setActive(false);
        reviewRepository.save(review);
        log.info("Review {} soft-deleted by user {}", reviewId, userId);

        updateProductRatingStats(review.getProductId());
    }



    // adminDeleteReview()

    public void adminDeleteReview(String reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        review.setActive(false);
        reviewRepository.save(review);
        log.warn("Admin soft-deleted review {}", reviewId);

        updateProductRatingStats(review.getProductId());
    }



    // getProductReviews()

    public PageResponse<ReviewResponse> getProductReviews(

            String productId, int page, int size, String sortBy, String sortDir,
            String currentUserId) {


        // whitelist-validation pattern, never pass raw client input straight into Sort.by().

        String validSortBy = "rating".equals(sortBy) ? "rating" : "createdAt";


        // Sorting Direction

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        
        // make pageable object
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
        Page<Review> reviewPage = reviewRepository.findByProductIdAndActiveTrue(productId, pageable);


        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(review -> {
                    ReviewResponse resp = reviewMapper.toResponse(review);
                    if (currentUserId != null) {
                        resp.setOwner(review.getUserId().equals(currentUserId));
                    }
                    return resp;
                })
                .toList();

        return PageResponse.of(
                responses,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()
        );
    }


    // getProductRatingsSummary()

    public ProductRatingsSummary getProductRatingsSummary(String productId) {


        // Run Aggregation

        List<ReviewRepository.RatingAggregationResult> breakdown =
                reviewRepository.getRatingBreakdown(productId);

//        System.out.println(breakdown);

        Map<Integer, Long> ratingMap = new HashMap<>();

        for (int star = 1; star <= 5; star++) {
            ratingMap.put(star, 0L);
        }



        for (var result : breakdown) {
//            System.out.println(result);
//            System.out.println(result.getId());
//            System.out.println(result.getCount());
            ratingMap.put(result.getId(), result.getCount());
        }

//        System.out.println(ratingMap);

        long totalRatings = ratingMap.values().stream().mapToLong(Long::longValue).sum();


        // calculate weighted sum

        long weightedSum = 0;

//        System.out.println(ratingMap);

        for (Map.Entry<Integer, Long> entry : ratingMap.entrySet()) {
            weightedSum += (long) entry.getKey() * entry.getValue();
        }


        // calculate average (round to 1 decimal)

        double average = totalRatings > 0
                ? Math.round((weightedSum / (double) totalRatings) * 10) / 10.0
                : 0.0;


        long totalReviews = reviewRepository
                .countByProductIdAndActiveTrueAndReviewTextIsNotNull(productId);

        return ProductRatingsSummary.builder()
                .productId(productId)
                .averageRating(average)
                .totalRatings(totalRatings)
                .totalReviews(totalReviews)
                .ratingBreakdown(ratingMap)
                .build();
    }



    // getMyReviews()

    public PageResponse<ReviewResponse> getMyReviews(String userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByUserIdAndActiveTrue(userId, pageable);

        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(review -> {
                    ReviewResponse resp = reviewMapper.toResponse(review);
                    resp.setOwner(true);
                    return resp;
                })
                .toList();

        return PageResponse.of(
                responses,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()
        );
    }



    // getAllReviewsForAdmin()

    public PageResponse<ReviewResponse> getAllReviewsForAdmin(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByActiveTrue(pageable);

        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();

        return PageResponse.of(
                responses,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()
        );
    }



    // PRIVATE HELPERS


    // updateProductRatingStats()

    private void updateProductRatingStats(String productId) {

        ProductRatingsSummary summary = getProductRatingsSummary(productId);

//        System.out.println(summary);

        productRepository.findById(productId).ifPresent(product -> {
            product.setAverageRating(summary.getAverageRating());
            product.setReviewCount((int) summary.getTotalRatings());

//            System.out.println(product);

            productRepository.save(product);
        });

        productCacheService.evictProduct(productId);

    }


    // hasVerifiedPurchase()

    private boolean hasVerifiedPurchase(String userId, String productId) {

        return orderRepository.existsByUserIdAndStatusAndItemsProductId(
                userId,
                OrderStatus.DELIVERED,
                productId
        );
    }


}
