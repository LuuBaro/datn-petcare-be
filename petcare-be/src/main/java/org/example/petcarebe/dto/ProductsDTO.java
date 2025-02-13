package org.example.petcarebe.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProductsDTO {
    private Long productId;
    private String productName;
    private float price;
    private String image;
    private String categoryName;  // Thêm thuộc tính loại sản phẩm

    public ProductsDTO(Long productId, String productName, String image, String categoryName) {
        this.productId = productId;
        this.productName = productName;
        this.image = image;
        this.categoryName = categoryName;
    }

    public void setPrice(Float price) {
        this.price = price != null ? price : 0f;  // Đảm bảo luôn là float, trả về 0f nếu giá trị null
    }
}
