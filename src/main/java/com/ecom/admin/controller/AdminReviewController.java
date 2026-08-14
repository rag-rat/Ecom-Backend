package com.ecom.admin.controller;


import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reviews", description = "Review moderation")
public class AdminReviewController {

    private final ReviewService reviewService;


    // getAllReviews() -- for admin

    @GetMapping
    @Operation(summary = "View all reviews across all products (admin)")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var response = reviewService.getAllReviewsForAdmin(page, size);

        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", response));
    }



    // deleteReview()  -- no ownership check (admin)

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete any review (moderation — no ownership check)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable String reviewId) {

        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review removed by moderation"));

    }

}
