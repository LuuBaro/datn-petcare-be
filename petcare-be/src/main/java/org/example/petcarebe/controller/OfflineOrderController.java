package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.dto.OfflineOrderDTO;
import org.example.petcarebe.dto.PointInfoDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.service.OfflineOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offline")
public class OfflineOrderController {

    @Autowired
    private OfflineOrderService offlineOrderService;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    @PostMapping("/orders")
    public ResponseEntity<OfflineOrderDTO.OfflineOrderResponse> createOfflineOrder(
            @Valid @RequestBody OfflineOrderDTO.OfflineOrderRequest request) {
        OfflineOrderDTO.OfflineOrderResponse response = offlineOrderService.createOfflineOrder(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/apply-discount")
    public ResponseEntity<OfflineOrderDTO.OfflineOrderResponse> applyDiscount(
            @Valid @RequestBody OfflineOrderDTO.OfflineOrderRequest request) {
        OfflineOrderDTO.OfflineOrderResponse response = offlineOrderService.applyDiscount(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDetails>> getAllProductDetails() {
        List<ProductDetails> products = productDetailsRepository.findAll();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/points")
    public ResponseEntity<PointInfoDTO> getPointsByPhone(@RequestParam("phone") String phone) {
        PointInfoDTO pointInfo = offlineOrderService.getPointsByPhone(phone);
        return ResponseEntity.ok(pointInfo);
    }

    // Thêm endpoint lấy tất cả hóa đơn
    @GetMapping("/all-orders")
    public ResponseEntity<List<OfflineOrderDTO.OfflineOrderResponse>> getAllOrders() {
        List<OfflineOrderDTO.OfflineOrderResponse> orders = offlineOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // Thêm endpoint sửa hóa đơn
    @PutMapping("/orders/{orderId}")
    public ResponseEntity<OfflineOrderDTO.OfflineOrderResponse> updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OfflineOrderDTO.OfflineOrderRequest request) {
        OfflineOrderDTO.OfflineOrderResponse response = offlineOrderService.updateOrder(orderId, request);
        return ResponseEntity.ok(response);
    }



}