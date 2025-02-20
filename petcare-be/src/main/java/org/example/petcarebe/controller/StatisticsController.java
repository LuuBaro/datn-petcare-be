package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final OrderService orderService;

    @GetMapping("/best-selling-products")
    public ResponseEntity<List<Map<String, Object>>> getBestSellingProducts() {
        return ResponseEntity.ok(orderService.getBestSellingProducts());
    }
}
