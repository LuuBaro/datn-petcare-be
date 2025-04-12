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
    private String orderId;
    private String momoOrderId; // ID đơn hàng từ MoMo
    private String momoTransId; // ID giao dịch MoMo
    private String momoAmount; // Số tiền giao dịch MoMo
    
    // Getter và setter thủ công cho momoOrderId
    public String getMomoOrderId() {
        return momoOrderId;
    }
    
    public void setMomoOrderId(String momoOrderId) {
        this.momoOrderId = momoOrderId;
    }
    
    // Getter và setter cho momoTransId
    public String getMomoTransId() {
        return momoTransId;
    }
    
    public void setMomoTransId(String momoTransId) {
        this.momoTransId = momoTransId;
    }
    
    // Getter và setter cho momoAmount
    public String getMomoAmount() {
        return momoAmount;
    }
    
    public void setMomoAmount(String momoAmount) {
        this.momoAmount = momoAmount;
    }
}

