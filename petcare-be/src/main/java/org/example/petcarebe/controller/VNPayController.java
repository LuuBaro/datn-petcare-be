package org.example.petcarebe.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.petcarebe.config.VNPayConfig;
import org.example.petcarebe.service.VNPayServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnp")
public class VNPayController {

    @Autowired
    private  VNPayServices vnPayServices; // Kiểm tra chắc chắn rằng VNPayServices đã được định nghĩa và đã có @Service

    // API để tạo đơn hàng và điều hướng đến VNPay
    @GetMapping("/create-payment")
    public String createPayment(
            @RequestParam("amount") int amount,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        // URL trở về sau khi thanh toán thành công
        String urlReturn = VNPayConfig.vnp_Returnurl;

        // Gọi dịch vụ VNPayServices để tạo đơn hàng
        String paymentUrl = vnPayServices.createOrder(amount, orderInfo, urlReturn); // Sử dụng phương thức createOrder đã định nghĩa trong VNPayServices
        return paymentUrl; // Trả về URL thanh toán
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> pay(@RequestBody Map<String, Object> paymentRequest, HttpServletRequest request) {
        try {
            // Kiểm tra xem "amount" có tồn tại và không null không
            Object amountObj = paymentRequest.get("amount");
            if (amountObj == null) {
                throw new IllegalArgumentException("Amount cannot be null");
            }

            // Ép kiểu và kiểm tra kiểu dữ liệu
            int amount;
            if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else {
                throw new IllegalArgumentException("Amount must be a number");
            }

            // Kiểm tra returnUrl
            String returnUrl = (String) paymentRequest.get("returnUrl");
            if (returnUrl == null || returnUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Return URL cannot be null or empty");
            }

            // Lấy baseUrl
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

            // Gọi VNPayServices
            String vnpayUrl = vnPayServices.createOrder(amount, "Thanh toán đơn hàng " + new Date(), returnUrl);

            // Trả về response
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", vnpayUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request parameters");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Payment creation failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/payment-result")
    public ResponseEntity<String> paymentResult(HttpServletRequest request) {
        int result = vnPayServices.orderReturn(request);
        if (result == 1) {
            // Redirect to the success URL
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "http://localhost:5173/user").build();
        } else {
            // Redirect to the checkout page on failure
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "http://localhost:5173/checkout").build();
        }
    }

}
