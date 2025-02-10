package org.example.petcarebe.controller;

import org.example.petcarebe.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
