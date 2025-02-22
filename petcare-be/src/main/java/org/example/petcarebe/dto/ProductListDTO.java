package org.example.petcarebe.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ProductListDTO {
    private Long productId;
    private String productName;
    private String description;
    private String image;
    private String categoryName;  // Thêm thuộc tính loại sản phẩm
    private String brandName;


}
