package org.example.petcarebe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomoRefundRequest {
    private String orderId;       // MoMo orderId cần hoàn tiền
    private String amount;        // Số tiền hoàn
    private String transId;       // ID giao dịch MoMo
    private String description;   // Mô tả lý do hoàn tiền
} 