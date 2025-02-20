package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.service.OrderService;
import org.example.petcarebe.service.ProductDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final OrderService orderService;

    @Autowired
    private ProductDetailsService productDetailsService;

    @GetMapping("/best-selling-products")
    public ResponseEntity<List<Map<String, Object>>> getBestSellingProducts() {
        return ResponseEntity.ok(orderService.getBestSellingProducts());
    }


    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

        BigDecimal revenue = orderService.getRevenueByDateRange(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }

    // Lấy tổng số lượng sản phẩm còn trong kho
    @GetMapping("/total-stock")
    public ResponseEntity<Integer> getTotalStock() {
        int totalStock = productDetailsService.getTotalStock();
        return ResponseEntity.ok(totalStock);
    }

    // Lấy thông tin số lượng sản phẩm còn trong kho
    @GetMapping("/stock-info")
    public ResponseEntity<List<Map<String, Object>>> getProductStockInfo() {
        List<Map<String, Object>> stockInfo = productDetailsService.getProductStockInfo();
        return ResponseEntity.ok(stockInfo);
    }


}
