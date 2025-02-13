package org.example.petcarebe.controller;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
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
        Orders order = orderService.checkout(request);

        // Tạo Map để chứa thông báo và dữ liệu
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thanh toán thành công");
        response.put("order", order.getOrderId());

        return ResponseEntity.ok(response);
    }



    // API Lấy tất cả đơn hàng
    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orderDTOList = orderService.getAllOrders();
        return ResponseEntity.ok(orderDTOList);
    }
}
