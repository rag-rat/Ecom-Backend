package com.ecom.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecom.exception.BusinessException;
import com.ecom.image.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.upload-folder}")
    private String uploadFolder;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;



    // uploadImage() --> validates and upload a single image file

    public ImageUploadResponse uploadImage(MultipartFile file) {

        validateFile(file);

        try {

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", uploadFolder,
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Image uploaded to Cloudinary: publicId={}", publicId);

            return ImageUploadResponse.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .build();


        } catch (IOException e) {

            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException("Image upload failed. Please try again.");
        }

    }



    // deleteImage()

    public void deleteImage(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return; // nothing to delete
        }


        try {

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: publicId={}", publicId);

        } catch (IOException e) {

            log.warn("Failed to delete image from Cloudinary: publicId={}, error={}",
                    publicId, e.getMessage());
        }

    }



    // validateFile()

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please select a file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("Image size must not exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(
                    "Only JPEG, PNG, and WebP images are allowed");
        }
    }
}
