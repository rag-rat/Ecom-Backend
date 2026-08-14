package com.ecom.image.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ImageUploadResponse {
    private String url;
    private String publicId;
}
