package com.ecom.category.service;


import com.ecom.category.dto.CategoryResponse;
import com.ecom.category.dto.CreateCategoryRequest;
import com.ecom.category.dto.UpdateCategoryRequest;
import com.ecom.category.entity.Category;
import com.ecom.category.mapper.CategoryMapper;
import com.ecom.category.repository.CategoryRepository;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;


    // createCategory()

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(
                    "A category with this name already exists", HttpStatus.CONFLICT);
        }

        Category category = Category.builder()
                .name(normalizedName)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} (id={})", saved.getName(), saved.getId());
        CategoryResponse dummy = categoryMapper.toResponse(saved);
        System.out.println(dummy);
        return dummy;

    }


    // updateCategory()

    public CategoryResponse updateCategory(String categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + categoryId));

        if (request.getName() != null) {
            String normalizedName = request.getName().trim();
            boolean nameChanged = !normalizedName.equalsIgnoreCase(category.getName());
            if (nameChanged && categoryRepository.existsByNameIgnoreCase(normalizedName)) {
                throw new BusinessException(
                        "A category with this name already exists", HttpStatus.CONFLICT);
            }
            category.setName(normalizedName);
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updated = categoryRepository.save(category);
        log.info("Category updated: {}", categoryId);
        return categoryMapper.toResponse(updated);
    }



    // deleteCategory()

    public void deleteCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + categoryId));

        category.setActive(false);
        categoryRepository.save(category);
        log.warn("Category soft-deleted: {} ({})", category.getName(), categoryId);
    }



    // getActiveCategoriesForCustomers()

    public List<CategoryResponse> getActiveCategoriesForCustomers() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }



    // getAllCategoriesForAdmin()

    public List<CategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }



    // getCategoryById()

    public CategoryResponse getCategoryById(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + categoryId));
        return categoryMapper.toResponse(category);
    }
}
