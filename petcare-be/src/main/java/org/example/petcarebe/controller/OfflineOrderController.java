package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.dto.OfflineOrderDTO;
import org.example.petcarebe.dto.PointInfoDTO;
import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.service.OfflineOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    // Tìm hóa đơn theo ngày
    @GetMapping("/orders-by-date")
    public ResponseEntity<List<OfflineOrderDTO.OfflineOrderResponse>> getOrdersByDateRange(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            // Đặt thời gian cuối ngày cho endDate (23:59:59)
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            endDate = cal.getTime();

            List<OfflineOrderDTO.OfflineOrderResponse> orders =
                    offlineOrderService.getOrdersByDateRange(startDate, endDate);
            return ResponseEntity.ok(orders);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/cart/add-product")
    public ResponseEntity<CartDetails> addProductToOfflineCart(
            @RequestParam Long userId,
            @RequestParam Long productDetailId,
            @RequestParam(defaultValue = "1") int quantity) {
        CartDetails cartDetail = offlineOrderService.addProductToOfflineCart(userId, productDetailId, quantity);
        return ResponseEntity.ok(cartDetail);
    }

    @DeleteMapping("/cart/remove-product")
    public ResponseEntity<Void> removeProductFromOfflineCart(
            @RequestParam Long userId,
            @RequestParam Long productDetailId) {
        offlineOrderService.removeProductFromOfflineCart(userId, productDetailId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartDetails>> getOfflineCartDetails(@RequestParam Long userId) {
        List<CartDetails> cartDetails = offlineOrderService.getOfflineCartDetails(userId);
        return ResponseEntity.ok(cartDetails);
    }
    



}