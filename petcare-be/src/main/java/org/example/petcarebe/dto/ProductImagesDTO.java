package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImagesDTO {
    private Long productImageId;
    private String imageUrl;
    private Long productDetailId;
    private String productName;
    private String colorValue;
    private String sizeValue;
    private float weightValue;

}
