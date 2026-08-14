package com.ecom.review.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.review.dto.CreateReviewRequest;
import com.ecom.review.dto.ProductRatingsSummary;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.dto.UpdateReviewRequest;
import com.ecom.review.service.ReviewService;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product ratings and reviews")
public class ReviewController {

    private final ReviewService reviewService;


    // getProductReviews()  --- both for logged-in user and anonymous users

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get paginated reviews for a product (public)")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String currentUserId = userDetails != null ? userDetails.getUsername() : null;

        var response = reviewService.getProductReviews(
                productId, page, size, sortBy, sortDir, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", response));
    }



    // getRatingsSummary()

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "Get rating summary (average, breakdown) for a product (public)")
    public ResponseEntity<ApiResponse<ProductRatingsSummary>> getRatingsSummary(
            @PathVariable String productId) {

        ProductRatingsSummary summary = reviewService.getProductRatingsSummary(productId);

        return ResponseEntity.ok(ApiResponse.success("Ratings summary fetched", summary));
    }



    // getMyReviews()

    @GetMapping("/my-reviews")
    @Operation(summary = "Get the current user's own reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var response = reviewService.getMyReviews(userDetails.getUsername(), page, size);

        return ResponseEntity.ok(ApiResponse.success("Your reviews fetched", response));
    }



    // createReview()

    @PostMapping
    @Operation(summary = "Create a review (one per product per user)")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse response =
                reviewService.createReview(request, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }



    // updateReview()

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update your own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {

        ReviewResponse response =
                reviewService.updateReview(reviewId, request, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }



    // deleteReview()

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete your own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String reviewId) {

        reviewService.deleteReview(reviewId, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }


}
