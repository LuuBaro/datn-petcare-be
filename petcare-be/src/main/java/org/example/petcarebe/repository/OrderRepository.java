package org.example.petcarebe.repository;

import org.example.petcarebe.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserUserId(Long userId);
    List<Orders> findByStatusOrder_StatusId(Long statusId);
    List<Orders> findAllByType(String type);

    // Tổng số đơn hàng OFFLINE hôm nay
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.orderDate = CURRENT_DATE")
    Long getTotalOfflineOrdersToday();

    // Tổng số đơn hàng ORDER ONLINE hôm nay
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND o.orderDate = CURRENT_DATE")
    Long getTotalOnlineOrdersToday();

    // Tổng số đơn hàng OFFLINE trong tháng này
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE) " +
            "AND FUNCTION('MONTH', o.orderDate) = FUNCTION('MONTH', CURRENT_DATE)")
    Long getTotalOfflineOrdersThisMonth();

    // Tổng số đơn hàng ORDER ONLINE trong tháng này
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE) " +
            "AND FUNCTION('MONTH', o.orderDate) = FUNCTION('MONTH', CURRENT_DATE)")
    Long getTotalOnlineOrdersThisMonth();

    // Tổng số đơn hàng OFFLINE trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOfflineOrdersByDateRange(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    // Tổng số đơn hàng ORDER ONLINE trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOnlineOrdersByDateRange(@Param("startDate") Date startDate,
                                         @Param("endDate") Date endDate);

    // Các phương thức hiện có vẫn giữ nguyên
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    @Query("SELECT FUNCTION('DATE', o.orderDate) AS date, " +
            "COALESCE(SUM(o.totalAmount), 0) AS revenue, " +
            "COUNT(o) AS order_count " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyRevenueByDateRange(@Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate = CURRENT_DATE")
    BigDecimal getRevenueToday();


    @Query(value = "SELECT COALESCE(SUM(o.total_amount), 0) " +
            "FROM Orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    BigDecimal getRevenueYesterday();

    // Tổng số đơn hàng OFFLINE hôm qua
    @Query(value = "SELECT COUNT(*) " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOfflineOrdersYesterday();

    // Tổng số đơn hàng ORDER ONLINE hôm qua
    @Query(value = "SELECT COUNT(*) " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOnlineOrdersYesterday();

    @Query("SELECT FUNCTION('DATE', o.orderDate) AS date, " +
            "SUM(CASE WHEN o.type = 'ORDER ONLINE' THEN 1 ELSE 0 END) AS online_orders, " +
            "SUM(CASE WHEN o.type = 'OFFLINE' THEN 1 ELSE 0 END) AS offline_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyOrderCountByType(@Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    @Query(value = "SELECT YEARWEEK(o.order_date) AS week, COALESCE(SUM(o.total_amount), 0) AS revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.order_date) " +
            "ORDER BY week ASC",
            nativeQuery = true)
    List<Object[]> getWeeklyRevenueByDateRange(@Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);

    // Tổng số đơn hàng theo tuần
    @Query("SELECT YEARWEEK(o.orderDate) AS week, " +
            "COUNT(o) AS order_count, " +
            "SUM(CASE WHEN o.type = 'ORDER ONLINE' THEN 1 ELSE 0 END) AS online_orders, " +
            "SUM(CASE WHEN o.type = 'OFFLINE' THEN 1 ELSE 0 END) AS offline_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.orderDate) " +
            "ORDER BY week ASC")
    List<Object[]> getWeeklyOrderCountByType(@Param("startDate") Date startDate,
                                             @Param("endDate") Date endDate);


    // Tổng số đơn hàng theo tháng (bao gồm offline và online) với truy vấn native SQL
    @Query(value = "SELECT CONCAT(year_value, '-', month_value) AS month, " +
            "COUNT(order_id) AS order_count, " +
            "SUM(CASE WHEN type = 'ORDER ONLINE' THEN 1 ELSE 0 END) AS online_orders, " +
            "SUM(CASE WHEN type = 'OFFLINE' THEN 1 ELSE 0 END) AS offline_orders " +
            "FROM (" +
            "    SELECT YEAR(o.order_date) AS year_value, " +
            "           MONTH(o.order_date) AS month_value, " +
            "           o.order_id, " +
            "           o.type " +
            "    FROM orders o " +
            "    WHERE o.payment_status = 'Đã thanh toán' " +
            "    AND o.order_date BETWEEN ?1 AND ?2" +
            ") AS temp " +
            "GROUP BY year_value, month_value " +
            "ORDER BY year_value, month_value", nativeQuery = true)
    List<Object[]> getMonthlyOrderCountByType(@Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE) " +
            "AND FUNCTION('MONTH', o.orderDate) = FUNCTION('MONTH', CURRENT_DATE)")
    BigDecimal getTotalRevenueThisMonth();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE)")
    BigDecimal getTotalRevenueThisYear();

    @Query("SELECT FUNCTION('DATE', o.orderDate) AS date, " +
            "COALESCE(SUM(o.totalAmount), 0) AS revenue, " +
            "COUNT(o) AS order_count " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = :year " +
            "AND FUNCTION('MONTH', o.orderDate) = :month " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyRevenueByMonth(@Param("year") int year,
                                          @Param("month") int month);

    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate = CURRENT_DATE")
    Long getTotalOrdersToday();

    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEARWEEK', o.orderDate) = FUNCTION('YEARWEEK', CURRENT_DATE)")
    Long getTotalOrdersThisWeek();

    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE) " +
            "AND FUNCTION('MONTH', o.orderDate) = FUNCTION('MONTH', CURRENT_DATE)")
    Long getTotalOrdersThisMonth();

    @Query(value = "SELECT COUNT(*) " +
            "FROM orders " +
            "WHERE payment_status = 'Đã thanh toán' " +
            "AND DATE(order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOrdersYesterday();

    @Query("SELECT COUNT(DISTINCT o.user) " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán'")
    Long getTotalCustomers();

    @Query(value = "SELECT o.user_id, u.full_name, u.phone, COUNT(*) as order_count " +
            "FROM orders o " +
            "JOIN users u ON o.user_id = u.user_id " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "GROUP BY o.user_id, u.full_name, u.phone " +
            "ORDER BY order_count DESC " +
            "LIMIT 5",
            nativeQuery = true)
    List<Object[]> getTopFiveCustomersByOrderCount();
}