package org.example.petcarebe.controller;

import org.example.petcarebe.dto.request.MomoRequest;
import org.example.petcarebe.service.MomoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/momo")
public class MomoController {

    @Autowired
    private MomoService momoService;

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody MomoRequest paymentRequest) {
        try {
            String response = momoService.createPaymentRequest(paymentRequest.getAmount(), paymentRequest.getReturnUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create MoMo payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/order-status/{orderId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String orderId) {
        try {
            String response = momoService.checkPaymentStatus(orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check MoMo payment status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 