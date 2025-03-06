package org.example.petcarebe.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

public class OfflineOrderDTO {
    @Data
    public static class OfflineOrderRequest {
        private Long userId;
        private List<OrderItemRequest> items;
        private String paymentMethod;
        private String customerPhone;
        private String customerName;
        private boolean accumulatePoints;
        private int pointsToUse;
    }

    @Data
    public static class OrderItemRequest {
        private Long productDetailId;
        private int quantity;
    }

    @Data
    public static class OfflineOrderResponse {
        private Long orderId;
        private String paymentMethod;
        private float totalAmount;
        private String status;
        private int pointsEarned;
        private Long userId;
        private String staffName;
        private int totalPoints;
        private String customerName;
        private String customerPhone;
        private Date orderDate;
        private List<OrderItemResponse> items;
    }

    @Data
    public static class OrderItemResponse {
        private String productName;
        private Long productDetailId;
        private float price;
        private int quantity;
        private String colorValue;
        private String sizeValue;
        private float weightValue;
    }
}