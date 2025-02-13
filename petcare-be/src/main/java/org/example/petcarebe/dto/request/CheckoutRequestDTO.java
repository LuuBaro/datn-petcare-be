package org.example.petcarebe.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    private Long userId;
    private String paymentMethod;
    private String shippingAddress;
    private float shippingCost;
    private Long voucherId; // Nếu có thể sử dụng voucher
    private List<OrderItemDTO> items;
}

