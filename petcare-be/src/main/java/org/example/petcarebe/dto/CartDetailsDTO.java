package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartDetailsDTO {
    private Long productDetailId;
    private String image;
    private String productName;
    private float price;
    private String colorValue;
    private String sizeValue;
    private float weightValue;
    private int quantityItem;
    private String description;
}
