package org.example.petcarebe.controller;


import lombok.RequiredArgsConstructor;
import org.example.petcarebe.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/statistics/spa")
@RequiredArgsConstructor
public class StatisticsSpaController {
    private final OrderService orderService;

    @GetMapping("/daily-revenue")
    public List<Object[]> getDailySpaRevenue(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        return orderService.getDailySpaRevenue(startDate, endDate);
    }
}
