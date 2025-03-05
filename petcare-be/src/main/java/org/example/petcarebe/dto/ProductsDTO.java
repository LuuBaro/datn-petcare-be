package org.example.petcarebe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.regex.PatternSyntaxException;

@Setter
@Getter
public class ProductsDTO {
    private Long productId;
    private String productName;
    private String description;
    private float price;
    private String image;
    private String categoryName;  // Thêm thuộc tính loại sản phẩm
    private String brandName;

    public ProductsDTO(Long productId, String productName, String description,String image, String categoryName, String brandName) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.image = image;
        this.categoryName = categoryName;
        this.brandName = brandName;
    }

    public void setPrice(Float price) {
        this.price = price != null ? price : 0f;  // Đảm bảo luôn là float, trả về 0f nếu giá trị null
    }

}
