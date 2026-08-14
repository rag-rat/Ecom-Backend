package com.ecom.category.repository;

import com.ecom.category.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<Category> findByIdAndActiveTrue(String id);

    boolean existsByNameIgnoreCase(String name);
}
