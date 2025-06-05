package org.example.petcarebe.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long orderId;
    private Date orderDate;
    private String paymentStatus;
    private String paymentMethod;
    private String shippingAddress;
    private float shippingCost;
    private float totalAmount;
    private String type;
    private int pointEarned;
    private int pointUsed;

    private Long userId;
    private String userName;
    private String phone;

    private Long statusId;
    private String statusName;

    private Long voucherId;
    
    // MoMo payment fields
    private String momoOrderId;
    private String momoTransId;

    private List<OrderDetailDTO> orderDetails;
}
