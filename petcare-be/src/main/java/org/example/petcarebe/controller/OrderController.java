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


    @DeleteMapping("/clearCart/{productDetailId}")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable Long productDetailId) {
        cartDetailsService.deleteCartDetails(productDetailId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa sản phẩm khỏi giỏ hàng thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Orders order = orderService.checkout(request);
            response.put("message", "Thanh toán thành công");
            response.put("order", order.getOrderId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    // API Lấy tất cả đơn hàng
    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orderDTOList = orderService.getAllOrders();
        return ResponseEntity.ok(orderDTOList);
    }



    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long orderId) {
        Orders order = orderService.cancelOrder(orderId);

        // Tạo phản hồi theo yêu cầu
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
            return ResponseEntity.ok().body(Collections.singletonMap("updatedOrder", updatedOrder)); // Đảm bảo JSON hợp lệ
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage())); // JSON đúng định dạng
        }
    }

}
