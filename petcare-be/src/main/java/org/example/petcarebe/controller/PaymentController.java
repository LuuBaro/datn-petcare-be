package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.MomoRefundRequest;
import org.example.petcarebe.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // API Test Thanh Toán cho toàn bộ giỏ hàng
    @PostMapping("/test/{userId}")
    public ResponseEntity<?> testPayment(@PathVariable Long userId) {
        return paymentService.processPayment(userId);
    }
    
    // API Hoàn tiền MoMo
    // LƯU Ý: MoMo chủ yếu dựa vào transId để xác định giao dịch cần hoàn tiền
    // orderId có thể là giá trị bất kỳ miễn là hợp lệ
    @PostMapping("/momo/refund")
    public ResponseEntity<?> refundMomoPayment(@RequestBody MomoRefundRequest request) {
        try {
            System.out.println("[CONTROLLER] Nhận yêu cầu hoàn tiền MoMo: " + 
                    "orderId=" + request.getOrderId() + 
                    ", transId=" + request.getTransId() + 
                    ", amount=" + request.getAmount());
            
            Map<String, Object> result = paymentService.refundMomoPayment(
                request.getOrderId(), 
                request.getAmount(), 
                request.getTransId(),
                request.getDescription()
            );
            
            // Log kết quả chi tiết
            System.out.println("[CONTROLLER] Kết quả hoàn tiền MoMo: success=" + result.get("success") + 
                    ", message=" + result.get("message"));
            
            if (result.containsKey("resultCode")) {
                System.out.println("[CONTROLLER] Mã kết quả MoMo: " + result.get("resultCode"));
            }
            
            if (result.containsKey("errorDescription")) {
                System.out.println("[CONTROLLER] Mô tả lỗi: " + result.get("errorDescription"));
            }
            
            // Chuyển đổi thời gian phản hồi sang chuỗi để dễ đọc
            if (result.containsKey("responseTime")) {
                result.put("responseTimeStr", result.get("responseTime").toString());
            }
            
            // Thêm timestamp server gửi kết quả về client
            result.put("serverResponseTime", new Date());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to refund MoMo payment");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", new Date());
            errorResponse.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // API Hoàn tiền MoMo dựa trên orderId nội bộ
    @PostMapping("/momo/refund/order/{orderId}")
    public ResponseEntity<?> refundMomoPaymentByOrderId(
            @PathVariable Long orderId, 
            @RequestBody Map<String, String> request) {
        try {
            System.out.println("\n===== NHẬN YÊU CẦU HOÀN TIỀN MOMO ===== ");
            System.out.println("OrderId: " + orderId);
            System.out.println("Request payload: " + request);
            
            String description = request.get("description");
            if (description == null || description.trim().isEmpty()) {
                description = "Hoàn tiền cho đơn hàng #" + orderId;
            }
            
            System.out.println("Mô tả hoàn tiền: " + description);
            System.out.println("Bắt đầu gọi paymentService.refundMomoPaymentByOrderId()");
            
            Map<String, Object> result = paymentService.refundMomoPaymentByOrderId(orderId, description);
            
            System.out.println("Kết quả hoàn tiền MOMO: " + result);
            System.out.println("===== KẾT THÚC YÊU CẦU HOÀN TIỀN MOMO =====\n");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("===== LỖI KHI HOÀN TIỀN MOMO =====");
            System.out.println("OrderId: " + orderId);
            System.out.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
            System.out.println("===== KẾT THÚC LỖI HOÀN TIỀN MOMO =====\n");
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to refund MoMo payment for order");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
