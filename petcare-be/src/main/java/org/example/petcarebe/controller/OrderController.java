package org.example.petcarebe.controller;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

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


}
