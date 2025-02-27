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
import java.time.LocalDate;
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

    // Lấy tổng doanh thu trong khoảng thời gian
    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

        BigDecimal revenue = orderService.getRevenueByDateRange(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }
    // Lấy tổng doanh thu trong ngày
    @GetMapping("/revenue/daily")
    public ResponseEntity<Map<Date, Map<String, Object>>> getDailyRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(orderService.getDailyRevenueByDateRange(startDate, endDate));
    }

    // tổng doanh thu từng ngày trong tháng
    @GetMapping("/revenue/daily-current-month")
    public ResponseEntity<Map<Date, Map<String, Object>>> getDailyRevenueCurrentMonth() {
        LocalDate now = LocalDate.now();
        Map<Date, Map<String, Object>> dailyStats = orderService.getDailyRevenueByMonth(
                now.getYear(),
                now.getMonthValue()
        );
        return ResponseEntity.ok(dailyStats);
    }

    // Lấy tổng doanh thu hôm nay
    @GetMapping("/revenue/today")
    public ResponseEntity<BigDecimal> getRevenueToday() {
        BigDecimal revenueToday = orderService.getRevenueToday();
        return ResponseEntity.ok(revenueToday);
    }

    // Lấy tổng doanh thu hôm qua
    @GetMapping("/revenue/yesterday")
    public ResponseEntity<BigDecimal> getRevenueYesterday() {
        BigDecimal revenueYesterday = orderService.getRevenueYesterday();
        return ResponseEntity.ok(revenueYesterday);
    }

    @GetMapping("/revenue/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(orderService.getWeeklyRevenueByDateRange(startDate, endDate));
    }


    // Lấy tổng doanh thu trong tháng
    @GetMapping("/revenue/month")
    public ResponseEntity<BigDecimal> getRevenueThisMonth() {
        return ResponseEntity.ok(orderService.getRevenueThisMonth());
    }

    // Lấy tổng doanh thu trong năm
    @GetMapping("/revenue/year")
    public ResponseEntity<BigDecimal> getRevenueThisYear() {
        return ResponseEntity.ok(orderService.getRevenueThisYear());
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

    // Tổng số đơn hàng trong ngày hôm nay (đã thanh toán hoặc hoàn thành)
    @GetMapping("/orders/today")
    public ResponseEntity<Long> getTotalOrdersToday() {
        Long totalOrders = orderService.getTotalOrdersToday();
        return ResponseEntity.ok(totalOrders);
    }

    // Tổng số đơn hàng trong tuần này (đã thanh toán hoặc hoàn thành)
    @GetMapping("/orders/week")
    public ResponseEntity<Long> getTotalOrdersThisWeek() {
        Long totalOrders = orderService.getTotalOrdersThisWeek();
        return ResponseEntity.ok(totalOrders);
    }

    // Tổng số đơn hàng trong tháng này (đã thanh toán hoặc hoàn thành)
    @GetMapping("/orders/month")
    public ResponseEntity<Long> getTotalOrdersThisMonth() {
        Long totalOrders = orderService.getTotalOrdersThisMonth();
        return ResponseEntity.ok(totalOrders);
    }

    // Tổng số đơn hàng hôm qua (đã thanh toán)
    @GetMapping("/orders/yesterday")
    public ResponseEntity<Long> getTotalOrdersYesterday() {
        Long totalOrders = orderService.getTotalOrdersYesterday();
        return ResponseEntity.ok(totalOrders);
    }

    //  Tổng số khách hàng
    @GetMapping("/total-customers")
    public ResponseEntity<Long> getTotalCustomers() {
        Long totalCustomers = orderService.getTotalCustomers();
        return ResponseEntity.ok(totalCustomers);
    }

    // Top 5 khách hàng mua nhiều nhất
    @GetMapping("/top-customers")
    public ResponseEntity<List<Map<String, Object>>> getTopFiveCustomersByOrderCount() {
        List<Map<String, Object>> topCustomers = orderService.getTopFiveCustomersByOrderCount();
        return ResponseEntity.ok(topCustomers);
    }

}
