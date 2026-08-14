package com.ecom.product.service;

import com.ecom.category.entity.Category;
import com.ecom.category.repository.CategoryRepository;
import com.ecom.common.dto.PageResponse;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.inventory.service.InventoryService;
import com.ecom.product.dto.*;
import com.ecom.product.entity.Product;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import com.ecom.product.repository.ProductSearchRepository;
import com.ecom.redis.ProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final ProductCacheService productCacheService;


    // createProduct()

    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountedPrice(request.getDiscountedPrice())
                .categoryId(category.getId())
                .categoryName(category.getName())

//                .fabric(request.getFabric())
//                .color(request.getColor())
//                .occasion(request.getOccasion())
//                .length(request.getLength())


                .brand(request.getBrand())
                .color(request.getColor())
                .productType(request.getProductType())
                .condition(request.getCondition())

                .tags(request.getTags())
                .featured(request.isFeatured())
                .active(true)
                .averageRating(0.0)
                .reviewCount(0)
                .build();


//        System.out.println(product.getPrice().getClass().getName());

        Product saved = productRepository.save(product);

        inventoryService.createInventory(saved.getId(), request.getInitialStock());

        log.info("Product created: {} (id={})", saved.getName(), saved.getId());

        return productMapper.toResponse(saved);
    }



    // updateProduct()

    public ProductResponse updateProduct(String productId, UpdateProductRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        if (request.getName() != null) product.setName(request.getName().trim());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountedPrice() != null) product.setDiscountedPrice(request.getDiscountedPrice());
//        if (request.getFabric() != null) product.setFabric(request.getFabric());
        if(request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getColor() != null) product.setColor(request.getColor());
//        if (request.getOccasion() != null) product.setOccasion(request.getOccasion());
        if(request.getColor() != null) product.setColor(request.getColor());
//        if (request.getLength() != null) product.setLength(request.getLength());
        if(request.getProductType() != null) product.setProductType(request.getProductType());
        if(request.getCondition() != null) product.setCondition(request.getCondition());
        if (request.getTags() != null) product.setTags(request.getTags());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getActive() != null) product.setActive(request.getActive());

        if (request.getCategoryId() != null &&
                !request.getCategoryId().equals(product.getCategoryId())) {
            Category newCategory = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: " + request.getCategoryId()));
            product.setCategoryId(newCategory.getId());
            product.setCategoryName(newCategory.getName());
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: {}", productId);

        productCacheService.evictProduct(productId);

        if (product.isFeatured() || Boolean.TRUE.equals(request.getFeatured())) {
            productCacheService.evictPopularProducts();
        }

        return productMapper.toResponse(updated);
    }



    // deleteProduct()

    // Soft delete only — preserves order history integrity. Past orders reference this product's data (embedded at order time, see Part 16), but the product itself should stop appearing in active browsing.

    public void deleteProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        product.setActive(false);
        productRepository.save(product);

        productCacheService.evictProduct(productId);

        if (product.isFeatured()) {
            productCacheService.evictPopularProducts();
        }

        log.warn("Product soft-deleted: {} ({})", product.getName(), productId);
    }



    // getProductById()

    public ProductResponse getProductById(String productId) {

        ProductResponse cached = productCacheService.getProduct(productId);

        if (cached != null) {
            return cached;
        }

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        ProductResponse response = productMapper.toResponse(product);
        productCacheService.cacheProduct(productId, response);
        return response;
    }



    // searchProducts()

    public PageResponse<ProductSummaryResponse> searchProducts(ProductSearchFilter filter) {

//        System.out.println(filter);

        var products = productSearchRepository.search(filter);
        long total = productSearchRepository.countSearch(filter);

//        System.out.println(products);
//        System.out.println(total);

        var summaries = products.stream()
                .map(productMapper::toSummaryResponse)
                .toList();

        int totalPages = (int) Math.ceil((double) total / filter.getSize());

//        System.out.println(summaries);

        return PageResponse.of(
                summaries,
                filter.getPage(),
                filter.getSize(),
                total,
                totalPages
        );
    }



    // getFeaturedProducts()

//    public PageResponse<ProductSummaryResponse> getFeaturedProducts(int page, int size) {
//        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
//        var productPage = productRepository.findByFeaturedTrueAndActiveTrue(pageable);
//
//        var summaries = productPage.getContent().stream()
//                .map(productMapper::toSummaryResponse)
//                .toList();
//
//        return PageResponse.of(
//                summaries,
//                productPage.getNumber(),
//                productPage.getSize(),
//                productPage.getTotalElements(),
//                productPage.getTotalPages()
//        );
//    }


    public PageResponse<ProductSummaryResponse> getFeaturedProducts(int page, int size) {

        if (page == 0 && size == 10) {
            List<ProductSummaryResponse> cached = productCacheService.getPopularProducts();
            if (cached != null) {
                return PageResponse.of(cached, 0, size, cached.size(), 1);
            }
        }

        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var productPage = productRepository.findByFeaturedTrueAndActiveTrue(pageable);

        var summaries = productPage.getContent().stream()
                .map(productMapper::toSummaryResponse)
                .toList();

        if (page == 0 && size == 10) {
            productCacheService.cachePopularProducts(summaries);
        }

        return PageResponse.of(
                summaries,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

}
