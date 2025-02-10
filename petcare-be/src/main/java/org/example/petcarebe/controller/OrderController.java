package org.example.petcarebe.controller;

import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orders> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Orders>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{statusId}")
    public ResponseEntity<List<Orders>> getOrdersByStatus(@PathVariable Long statusId) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(statusId));
    }

//    @PostMapping
//    public ResponseEntity<Orders> createOrder(@RequestBody Orders orders) {
//        return ResponseEntity.ok(orderService.createOrder(orders));
//    }

//    @PutMapping("/{id}")
//    public ResponseEntity<Orders> updateOrder(@PathVariable Long id, @RequestBody Orders orders) {
//        orders.setOrderId(id);
//        return ResponseEntity.ok(orderService.updateOrder(orders));
//    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
//        orderService.deleteOrder(id);
//        return ResponseEntity.ok().build();
//    }
}