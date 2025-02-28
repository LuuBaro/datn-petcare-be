package org.example.petcarebe.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    private Long userId;
    private String paymentMethod;
    private String paymentStatus;
    private String shippingAddress;
    private float shippingCost;
    private Long voucherId; // Nếu có thể sử dụng voucher
    private String type; // Loại đơn hàng
    private List<OrderItemDTO> items;
}

