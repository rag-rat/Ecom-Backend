package com.ecom.product.service;

import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.image.dto.ImageUploadResponse;
import com.ecom.image.service.CloudinaryService;
import com.ecom.product.dto.ProductResponse;
import com.ecom.product.entity.Product;
import com.ecom.product.mapper.ProductMapper;
import com.ecom.product.repository.ProductRepository;
import com.ecom.redis.ProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;
    private final ProductCacheService productCacheService;



    // uploadProductImage()

    public ProductResponse uploadProductImage(
            String productId, MultipartFile file, boolean isPrimary) {


        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));


        ImageUploadResponse uploadResult = cloudinaryService.uploadImage(file);


        List<Product.ProductImage> images = product.getImages();
        if (images == null) {
            images = new ArrayList<>();
        }


        if (isPrimary) {
            images.forEach(img -> img.setPrimary(false));
        }


        boolean shouldBePrimary = isPrimary || images.isEmpty();

        Product.ProductImage newImage = Product.ProductImage.builder()
                .url(uploadResult.getUrl())
                .publicId(uploadResult.getPublicId())
                .primary(shouldBePrimary)
                .build();


        images.add(newImage);
        product.setImages(images);

        Product updated = productRepository.save(product);
        log.info("Image added to product {}: publicId={}", productId, uploadResult.getPublicId());

        productCacheService.evictProduct(productId);

        if (product.isFeatured()) {
            productCacheService.evictPopularProducts();
        }

        return productMapper.toResponse(updated);
    }



    // deleteProductImage()

    public ProductResponse deleteProductImage(String productId, String publicId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        List<Product.ProductImage> images = product.getImages();

        if (images == null || images.isEmpty()) {
            throw new BusinessException("This product has no images to delete");
        }


        boolean removedWasPrimary = images.stream()
                .anyMatch(img -> img.getPublicId().equals(publicId) && img.isPrimary());


        images.removeIf(img -> img.getPublicId().equals(publicId));


        if (removedWasPrimary && !images.isEmpty()
                && images.stream().noneMatch(Product.ProductImage::isPrimary)) {
            images.get(0).setPrimary(true);
        }


        product.setImages(images);
        Product updated = productRepository.save(product);


        cloudinaryService.deleteImage(publicId);

        log.info("Image removed from product {}: publicId={}", productId, publicId);

        productCacheService.evictProduct(productId);

        if (product.isFeatured()) {
            productCacheService.evictPopularProducts();
        }

        return productMapper.toResponse(updated);
    }



    // setPrimaryImage()

    public ProductResponse setPrimaryImage(String productId, String publicId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        List<Product.ProductImage> images = product.getImages();

        if (images == null || images.stream().noneMatch(img -> img.getPublicId().equals(publicId))) {
            throw new ResourceNotFoundException("Image not found on this product: " + publicId);
        }

        images.forEach(img -> img.setPrimary(img.getPublicId().equals(publicId)));
        product.setImages(images);

        Product updated = productRepository.save(product);

        log.info("Primary image set for product {}: publicId={}", productId, publicId);

        productCacheService.evictProduct(productId);

        if (product.isFeatured()) {
            productCacheService.evictPopularProducts();
        }

        return productMapper.toResponse(updated);

    }

}

