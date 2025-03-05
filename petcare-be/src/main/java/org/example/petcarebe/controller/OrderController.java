package org.example.petcarebe.controller;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.CartDetailsService;
import org.example.petcarebe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartDetailsService cartDetailsService;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequestDTO request) {
        System.out.println("Received checkout request: " + request);
        Map<String, Object> response = new HashMap<>();
        try {
            // Kiểm tra dữ liệu đầu vào
            if (request.getUserId() == null || request.getPaymentMethod() == null) {
                response.put("message", "Thiếu thông tin người dùng hoặc phương thức thanh toán");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Gọi OrderService để tạo đơn hàng
            Orders order = orderService.checkout(request);

            // Trả về response với thông tin đơn hàng
            response.put("message", "Đặt hàng thành công");
            response.put("orderId", order.getOrderId());
            response.put("paymentStatus", order.getPaymentStatus()); // Thêm paymentStatus vào response
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Các phương thức khác giữ nguyên
    @DeleteMapping("/clearCart/{productDetailId}")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable Long productDetailId) {
        cartDetailsService.deleteCartDetails(productDetailId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa sản phẩm khỏi giỏ hàng thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orderDTOList = orderService.getAllOrders();
        return ResponseEntity.ok(orderDTOList);
    }

    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long orderId) {
        Orders order = orderService.cancelOrder(orderId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đơn hàng đã được hủy thành công");
        response.put("orderId", order.getOrderId());
        response.put("status", order.getStatusOrder().getStatusName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/{statusId}")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @PathVariable Long statusId) {
        try {
            Orders updatedOrder = orderService.updateOrderStatus(orderId, statusId);
            return ResponseEntity.ok().body(Collections.singletonMap("updatedOrder", updatedOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> requestBody
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String paymentStatus = requestBody.get("paymentStatus");
            if (paymentStatus == null) {
                response.put("message", "paymentStatus is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            Orders order = orderService.updatePaymentStatus(orderId, paymentStatus);
            response.put("message", "Cập nhật trạng thái thanh toán thành công");
            response.put("orderId", order.getOrderId());
            response.put("paymentStatus", order.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}