package org.example.petcarebe.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ProductSummaryDTO {
    private Long productDetailId;
    private String productName;
    private String productImage;
    private Float price;
    private Integer quantity;
    private String description;
    private String brandName;
    private String categoryName;

    // Constructor phù hợp với JPQL
    public ProductSummaryDTO(Long productDetailId, String productName, String productImage, Float price,
                             Integer quantity, String description, String brandName, String categoryName) {
        this.productDetailId = productDetailId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.quantity = quantity;
        this.description = description;
        this.brandName = brandName;
        this.categoryName = categoryName;
    }
}
