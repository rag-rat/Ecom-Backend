package com.ecom.review.mapper;

import com.ecom.review.dto.ReviewResponse;
import com.ecom.review.entity.Review;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review review);

}