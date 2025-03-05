package org.example.petcarebe.dto.request;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productDetailId;
    private int quantity;
    private float price;
}
