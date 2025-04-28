package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.MedicalRecordDTO;
import org.example.petcarebe.dto.VetOrderDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.model.User;
import org.example.petcarebe.service.VetOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vet-orders")
@RequiredArgsConstructor
public class VetOrderController {

    private final VetOrderService vetOrderService;

    // Endpoint để tạo đơn hàng mới
    @PostMapping("/create")
    public ResponseEntity<Orders> createVetOrder(
            @RequestParam Long userId,
            @RequestBody List<MedicalRecordDTO> medicalRecordDTOs,
            @RequestParam String paymentMethod) {
        try {
            Orders order = vetOrderService.createVetOrder(userId, medicalRecordDTOs, paymentMethod);
            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint để xử lý thanh toán
    @PutMapping("/payment/{orderId}")
    public ResponseEntity<Orders> processPayment(
            @PathVariable Long orderId,
            @RequestParam String paymentStatus) {
        try {
            Orders updatedOrder = vetOrderService.processPayment(orderId, paymentStatus);
            return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint để lấy thông tin đơn hàng theo ID
    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderById(@PathVariable Long orderId) {
        Optional<Orders> order = vetOrderService.getOrderById(orderId);
        return order.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Orders>> getOrdersByUserId(@PathVariable Long userId) {
        List<Orders> orders = vetOrderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/userDTO/{userId}")
    public ResponseEntity<?> getVetOrdersByUserId(@PathVariable Long userId) {
        try {
            List<VetOrderDTO> vetOrderDTOs = vetOrderService.getVetOrderDTOsByUserId(userId);
            if (vetOrderDTOs.isEmpty()) {
                return ResponseEntity.status(404).body("Không tìm thấy hóa đơn");
            }
            return ResponseEntity.ok(vetOrderDTOs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // Lấy tất cả VetOrderDTO với type = "VET_SERVICE"
    @GetMapping("/all-vet-service")
    public ResponseEntity<List<VetOrderDTO>> getAllVetOrdersByTypeVetService() {
        List<VetOrderDTO> vetOrders = vetOrderService.getAllVetOrdersByTypeVetService();
        return new ResponseEntity<>(vetOrders, HttpStatus.OK);
    }

    @GetMapping("/getUserName/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        try {
            User user = vetOrderService.getUserNameByUserId(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}