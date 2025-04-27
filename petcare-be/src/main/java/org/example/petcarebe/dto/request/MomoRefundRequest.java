package org.example.petcarebe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu hoàn tiền MoMo
 * Lưu ý: Mặc dù cần cung cấp cả orderId và transId, 
 * transId là thông số quan trọng nhất và đủ để xác định giao dịch cần hoàn tiền.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomoRefundRequest {
    private String orderId;       // MoMo orderId cần hoàn tiền (có thể là giá trị bất kỳ miễn là hợp lệ)
    private String amount;        // Số tiền cần hoàn (cần khớp với số tiền đã thanh toán)
    private String transId;       // ID giao dịch MoMo (bắt buộc và phải chính xác)
    private String description;   // Mô tả lý do hoàn tiền
} 