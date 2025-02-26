package org.example.petcarebe.repository;

import org.example.petcarebe.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserUserId(Long userId);
    List<Orders> findByStatusOrder_StatusId(Long statusId);
//    List<Orders> findByPaymentStatus(Long paymentStatus);
    List<Orders> findByType(String type);


    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);


    @Query("SELECT FUNCTION('DATE', o.orderDate) AS date, COALESCE(SUM(o.totalAmount), 0) AS revenue " +
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


    @Query(value = "SELECT YEARWEEK(o.order_date) AS week, COALESCE(SUM(o.total_amount), 0) AS revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.order_date) " +
            "ORDER BY week ASC",
            nativeQuery = true)
    List<Object[]> getWeeklyRevenueByDateRange(@Param("startDate") Date startDate,
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

    @Query("SELECT FUNCTION('DATE', o.orderDate) AS date, COALESCE(SUM(o.totalAmount), 0) AS revenue " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = :year " +
            "AND FUNCTION('MONTH', o.orderDate) = :month " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyRevenueByMonth(@Param("year") int year,
                                          @Param("month") int month);


    // Tổng số đơn hàng trong ngày hôm nay (paymentStatus = "Đã thanh toán")
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate = CURRENT_DATE")
    Long getTotalOrdersToday();

    // Tổng số đơn hàng trong tuần này (paymentStatus = "Đã thanh toán")
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEARWEEK', o.orderDate) = FUNCTION('YEARWEEK', CURRENT_DATE)")
    Long getTotalOrdersThisWeek();

    // Tổng số đơn hàng trong tháng này (paymentStatus = "Đã thanh toán")
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND FUNCTION('YEAR', o.orderDate) = FUNCTION('YEAR', CURRENT_DATE) " +
            "AND FUNCTION('MONTH', o.orderDate) = FUNCTION('MONTH', CURRENT_DATE)")
    Long getTotalOrdersThisMonth();
}




