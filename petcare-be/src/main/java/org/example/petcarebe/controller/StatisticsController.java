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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final OrderService orderService;

    @Autowired
    private ProductDetailsService productDetailsService;

    // Lấy danh sách các sản phẩm bán chạy nhất
    @GetMapping("/best-selling-products")
    public ResponseEntity<List<Map<String, Object>>> getBestSellingProducts() {
        return ResponseEntity.ok(orderService.getBestSellingProducts());
    }

    // Lấy danh sách top 5 sản phẩm được yêu thích nhất
    @GetMapping("/top-favorite-products")
    public ResponseEntity<List<Map<String, Object>>> getTopFavoriteProducts() {
        return ResponseEntity.ok(orderService.getTopFiveFavoriteProducts());
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

    // Tổng doanh thu từng ngày trong tháng hiện tại
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

    // Lấy số lượng đơn hàng hàng ngày theo loại (ONLINE và OFFLINE)
    @GetMapping("/orders/daily-by-type")
    public ResponseEntity<Map<Date, Map<String, Long>>> getDailyOrderCountByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Map<Date, Map<String, Long>> dailyOrderStats = orderService.getDailyOrderCountByType(startDate, endDate);
        return ResponseEntity.ok(dailyOrderStats);
    }

    // Lấy số lượng đơn hàng hàng tuần theo loại (ONLINE và OFFLINE)
    @GetMapping("/orders/weekly-by-type")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyOrderCountByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(orderService.getWeeklyOrderCountByType(startDate, endDate));
    }

    // Lấy số lượng đơn hàng hàng tháng theo loại (ONLINE và OFFLINE)
    @GetMapping("/orders/monthly-by-type")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyOrderCountByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(orderService.getMonthlyOrderCountByType(startDate, endDate));
    }

    // Lấy số liệu đơn hàng hôm qua
    @GetMapping("/yesterday-stats")
    public ResponseEntity<Map<String, Long>> getYesterdayOrderStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("offlineOrdersYesterday", orderService.getTotalOfflineOrdersYesterday());
        stats.put("onlineOrdersYesterday", orderService.getTotalOnlineOrdersYesterday());
        return ResponseEntity.ok(stats);
    }

    // Lấy tổng doanh thu hàng tuần
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

    // Tổng số đơn hàng trong ngày hôm nay (bao gồm OFFLINE và ORDER ONLINE)
    @GetMapping("/orders/today")
    public ResponseEntity<Map<String, Long>> getTotalOrdersToday() {
        Map<String, Long> response = new HashMap<>();
        response.put("totalOrders", orderService.getTotalOrdersToday());
        response.put("offlineOrders", orderService.getTotalOfflineOrdersToday());
        response.put("onlineOrders", orderService.getTotalOnlineOrdersToday());
        return ResponseEntity.ok(response);
    }

    // Tổng số đơn hàng trong tuần này (đã thanh toán hoặc hoàn thành)
    @GetMapping("/orders/week")
    public ResponseEntity<Long> getTotalOrdersThisWeek() {
        Long totalOrders = orderService.getTotalOrdersThisWeek();
        return ResponseEntity.ok(totalOrders);
    }

    // Tổng số đơn hàng trong tháng này (bao gồm OFFLINE và ORDER ONLINE)
    @GetMapping("/orders/month")
    public ResponseEntity<Map<String, Long>> getTotalOrdersThisMonth() {
        Map<String, Long> response = new HashMap<>();
        response.put("totalOrders", orderService.getTotalOrdersThisMonth());
        response.put("offlineOrders", orderService.getTotalOfflineOrdersThisMonth());
        response.put("onlineOrders", orderService.getTotalOnlineOrdersThisMonth());
        return ResponseEntity.ok(response);
    }

    // Tổng số đơn hàng hôm qua (đã thanh toán)
    @GetMapping("/orders/yesterday")
    public ResponseEntity<Long> getTotalOrdersYesterday() {
        Long totalOrders = orderService.getTotalOrdersYesterday();
        return ResponseEntity.ok(totalOrders);
    }

    // Tổng số đơn hàng trong khoảng thời gian (bao gồm OFFLINE và ORDER ONLINE)
    @GetMapping("/orders/range")
    public ResponseEntity<Map<String, Long>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Map<String, Long> response = new HashMap<>();
        response.put("offlineOrders", orderService.getTotalOfflineOrdersByDateRange(startDate, endDate));
        response.put("onlineOrders", orderService.getTotalOnlineOrdersByDateRange(startDate, endDate));
        return ResponseEntity.ok(response);
    }

    // Tổng số khách hàng
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

    // Tổng doanh thu offline và online hôm nay
    @GetMapping("/revenue/today/by-type")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueTodayByType() {
        Map<String, BigDecimal> revenueByType = orderService.getRevenueTodayByType();
        return ResponseEntity.ok(revenueByType);
    }

    // Tổng doanh thu offline và online hôm qua
    @GetMapping("/revenue/yesterday/by-type")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueYesterdayByType() {
        Map<String, BigDecimal> revenueByType = orderService.getRevenueYesterdayByType();
        return ResponseEntity.ok(revenueByType);
    }

    // Tổng doanh thu offline và online tháng này
    @GetMapping("/revenue/this-month/by-type")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueThisMonthByType() {
        Map<String, BigDecimal> revenueByType = orderService.getRevenueThisMonthByType();
        return ResponseEntity.ok(revenueByType);
    }

    // Doanh thu online và offline từng ngày trong khoảng thời gian
    @GetMapping("/revenue/daily-by-type")
    public ResponseEntity<Map<Date, Map<String, BigDecimal>>> getDailyRevenueByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Map<Date, Map<String, BigDecimal>> dailyRevenueByType = orderService.getDailyRevenueByType(startDate, endDate);
        return ResponseEntity.ok(dailyRevenueByType);
    }

    // Lấy tổng doanh thu từng tháng trong khoảng thời gian xác định
    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        System.out.println("Nhận startDate: " + startDate + ", endDate: " + endDate);
        List<Map<String, Object>> monthlyRevenue = orderService.getMonthlyRevenueByDateRange(startDate, endDate);
        System.out.println("Kết quả: " + monthlyRevenue);
        return ResponseEntity.ok(monthlyRevenue);
    }

    // Lấy tổng doanh thu từng tháng theo loại đơn trong khoảng thời gian xác định
    @GetMapping("/revenue/monthly-by-type")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenueByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return ResponseEntity.ok(orderService.getMonthlyRevenueByOrderType(startDate, endDate));
    }


    // Lấy doanh thu hàng tuần theo loại (ONLINE và OFFLINE) trong khoảng thời gian xác định
    @GetMapping("/revenue/weekly-by-type")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyRevenueByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Map<String, Object>> weeklyRevenueByType = orderService.getWeeklyRevenueByTypeAndDateRange(startDate, endDate);
        return ResponseEntity.ok(weeklyRevenueByType);
    }
}