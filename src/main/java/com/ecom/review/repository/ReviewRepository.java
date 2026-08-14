package com.ecom.review.repository;

import com.ecom.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;



@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByProductIdAndUserId(String productId, String userId);

    boolean existsByProductIdAndUserIdAndActiveTrue(String productId, String userId);

    Page<Review> findByProductIdAndActiveTrue(String productId, Pageable pageable);

    long countByProductIdAndActiveTrue(String productId);

    long countByProductIdAndActiveTrueAndReviewTextIsNotNull(String productId);

    Page<Review> findByUserIdAndActiveTrue(String userId, Pageable pageable);

    Page<Review> findByActiveTrue(Pageable pageable);



    @Aggregation(pipeline = {
            "{ $match: { 'product_id': ?0, 'is_active': true } }",
            "{ $group: { '_id': '$rating', 'count': { $sum: 1 } } }",
            "{ $sort: { '_id': 1 } }"
    })
    List<RatingAggregationResult> getRatingBreakdown(String productId);

    interface RatingAggregationResult {
//        @Field("_id")
        @org.springframework.beans.factory.annotation.Value("#{target._id}")
        Integer getId();
        Long getCount();
    }

}
