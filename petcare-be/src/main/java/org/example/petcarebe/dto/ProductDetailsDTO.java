package org.example.petcarebe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
@NoArgsConstructor
public class ProductDetailsDTO {
    private Long productDetailId;
    private String productName;
    private float price;
    private String colorValue;
    private String sizeValue;
    private float weightValue;
    private int quantity;
    private String productImage;
    private String description;
    private List<String> imageUrls;  // Để có thể lưu trữ ảnh nếu cần
    private String categoryName;

    // Constructor phù hợp với truy vấn JPQL của bạn
    public ProductDetailsDTO(Long productDetailId, String productName,String productImage, float price,
                             String colorValue, String sizeValue, float weightValue, Integer quantity, String description,
                             String categoryName) {
        this.productDetailId = productDetailId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.colorValue = colorValue;
        this.sizeValue = sizeValue;
        this.weightValue = weightValue;
        this.quantity = quantity;
        this.description = description;
        this.categoryName = categoryName;
    }




}
