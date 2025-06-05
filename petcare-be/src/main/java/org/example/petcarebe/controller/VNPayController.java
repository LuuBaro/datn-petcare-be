package org.example.petcarebe.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.petcarebe.config.VNPayConfig;
import org.example.petcarebe.service.VNPayServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(VNPayController.class);

    @Autowired
    private VNPayServices vnPayServices; // Kiểm tra chắc chắn rằng VNPayServices đã được định nghĩa và đã có @Service

    // API để tạo đơn hàng và điều hướng đến VNPay
    @GetMapping("/create-payment")
    public String createPayment(
            @RequestParam("amount") int amount,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        // URL trở về sau khi thanh toán thành công
        String urlReturn = request.getParameter("returnUrl");
        if (urlReturn == null || urlReturn.isEmpty()) {
            urlReturn = "http://localhost:5173/checkout"; // Fallback URL
        }

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
                try {
                    amount = Integer.parseInt(amountObj.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Amount must be a valid number");
                }
            }

            // Kiểm tra returnUrl
            String returnUrl = (String) paymentRequest.get("returnUrl");
            if (returnUrl == null || returnUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Return URL cannot be null or empty");
            }

            logger.info("Creating VNPay payment: amount={}, returnUrl={}", amount, returnUrl);

            // Gọi VNPayServices
            String vnpayUrl = vnPayServices.createOrder(amount, "Thanh toán đơn hàng " + new Date(), returnUrl);
            logger.info("VNPay URL generated: {}", vnpayUrl);

            // Trả về response
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", vnpayUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request parameters");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Payment creation failed: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Payment creation failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/payment-result")
    public ResponseEntity<Map<String, Object>> paymentResult(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Lấy tất cả các tham số từ request để debugging
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String key : requestParams.keySet()) {
                logger.info("VNPay callback parameter: {}={}", key, String.join(", ", requestParams.get(key)));
            }

            // Kiểm tra kết quả thanh toán
            int result = vnPayServices.orderReturn(request);
            
            // Lấy các tham số quan trọng
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TxnRef = request.getParameter("vnp_TxnRef");
            
            // Lưu các tham số này vào response
            response.put("vnp_ResponseCode", vnp_ResponseCode);
            response.put("vnp_TxnRef", vnp_TxnRef);
            
            if (result == 1) {
                // Thanh toán thành công
                logger.info("VNPay payment successful: vnp_TxnRef={}, vnp_ResponseCode={}", vnp_TxnRef, vnp_ResponseCode);
                response.put("status", "success");
                response.put("message", "Thanh toán thành công");
            } else if (result == 0) {
                // Thanh toán thất bại
                logger.warn("VNPay payment failed: vnp_TxnRef={}, vnp_ResponseCode={}", vnp_TxnRef, vnp_ResponseCode);
                response.put("status", "failed");
                response.put("message", "Thanh toán thất bại");
            } else {
                // Lỗi xác thực chữ ký
                logger.error("VNPay signature verification failed: vnp_TxnRef={}", vnp_TxnRef);
                response.put("status", "error");
                response.put("message", "Lỗi xác thực chữ ký");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing VNPay payment result: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Lỗi xử lý kết quả thanh toán: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
