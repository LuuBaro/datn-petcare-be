package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.MomoRefundRequest;
import org.example.petcarebe.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/momo/refund")
    public ResponseEntity<?> refundMomoPayment(@RequestBody MomoRefundRequest request) {
        try {
            Map<String, Object> result = paymentService.refundMomoPayment(
                request.getOrderId(), 
                request.getAmount(), 
                request.getTransId(),
                request.getDescription()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to refund MoMo payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
