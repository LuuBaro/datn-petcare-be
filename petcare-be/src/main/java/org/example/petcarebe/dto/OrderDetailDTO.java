package org.example.petcarebe.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDTO {
    private Long orderDetailId;
    private int quantity;
    private float price;
    private String imageUrl;
    private String colorValue;
    private String sizeValue;
    private float weightValue;

    private Long productDetailId;
    private String productName;
}
