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

    // Tìm tất cả các đơn hàng liên quan đến một người dùng cụ thể dựa trên ID người dùng.
    List<Orders> findByUserUserId(Long userId);

    // Lấy danh sách các đơn hàng dựa trên ID trạng thái cụ thể.
    List<Orders> findByStatusOrder_StatusId(Long statusId);

    // Lấy tất cả các đơn hàng thuộc một loại cụ thể (ví dụ: "OFFLINE" hoặc "ORDER ONLINE").
    List<Orders> findAllByType(String type);

    // Tìm các đơn hàng theo loại và ID trạng thái, cho phép lọc theo cả hai tiêu chí.
    List<Orders> findByTypeAndStatusOrderStatusId(String type, Long statusId);

    // Lấy các đơn hàng theo loại, ID trạng thái và khoảng thời gian (từ startDate đến endDate).
    List<Orders> findByTypeAndStatusOrderStatusIdAndOrderDateBetween(
            String type, Long statusId, Date startDate, Date endDate);

    // Đếm tổng số đơn hàng trong ngày hôm nay với trạng thái thanh toán là "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTotalOrdersToday();

    // Đếm tổng số đơn hàng OFFLINE trong ngày hôm nay với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTotalOfflineOrdersToday();

    // Đếm tổng số đơn hàng ORDER ONLINE trong ngày hôm nay với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTotalOnlineOrdersToday();

    // Đếm tổng số đơn hàng OFFLINE trong tháng hiện tại với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Long getTotalOfflineOrdersThisMonth();

    // Đếm tổng số đơn hàng ORDER ONLINE trong tháng hiện tại với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Long getTotalOnlineOrdersThisMonth();

    // Đếm tổng số đơn hàng OFFLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOfflineOrdersByDateRange(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    // Đếm tổng số đơn hàng ORDER ONLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOnlineOrdersByDateRange(@Param("startDate") Date startDate,
                                         @Param("endDate") Date endDate);

    // Tính tổng doanh thu trong khoảng thời gian xác định từ các đơn hàng "Đã thanh toán".
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    // Lấy doanh thu hàng ngày và số lượng đơn hàng trong khoảng thời gian xác định.
    @Query("SELECT CAST(o.orderDate AS date) AS date, " +
            "COALESCE(SUM(o.totalAmount), 0) AS revenue, " +
            "COUNT(o) AS order_count " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyRevenueByDateRange(@Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    // Tính doanh thu trong ngày hôm nay từ các đơn hàng "Đã thanh toán".
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    BigDecimal getRevenueToday();

    // Tính doanh thu của ngày hôm qua từ các đơn hàng "Đã thanh toán" (sử dụng native SQL).
    @Query(value = "SELECT COALESCE(SUM(o.total_amount), 0) " +
            "FROM Orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    BigDecimal getRevenueYesterday();

    // Đếm tổng số đơn hàng OFFLINE của ngày hôm qua với trạng thái "Đã thanh toán" (sử dụng native SQL).
    @Query(value = "SELECT COUNT(*) " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOfflineOrdersYesterday();

    // Đếm tổng số đơn hàng ORDER ONLINE của ngày hôm qua với trạng thái "Đã thanh toán" (sử dụng native SQL).
    @Query(value = "SELECT COUNT(*) " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'ORDER ONLINE' " +
            "AND DATE(o.order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOnlineOrdersYesterday();

    // Lấy số lượng đơn hàng hàng ngày theo loại (ONLINE và OFFLINE) trong khoảng thời gian xác định.
    @Query("SELECT DATE(o.orderDate) AS date, " +
            "SUM(CASE WHEN o.type = 'ORDER ONLINE' THEN 1 ELSE 0 END) AS online_orders, " +
            "SUM(CASE WHEN o.type = 'OFFLINE' THEN 1 ELSE 0 END) AS offline_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyOrderCountByType(@Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    // Lấy doanh thu hàng tuần trong khoảng thời gian xác định (sử dụng native SQL).
    @Query(value = "SELECT YEARWEEK(o.order_date) AS week, COALESCE(SUM(o.total_amount), 0) AS revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.order_date) " +
            "ORDER BY week ASC",
            nativeQuery = true)
    List<Object[]> getWeeklyRevenueByDateRange(@Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);

    // Lấy số lượng đơn hàng hàng tuần theo loại (ONLINE và OFFLINE) trong khoảng thời gian xác định.
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

    // Lấy số lượng đơn hàng hàng tháng theo loại (ONLINE và OFFLINE) trong khoảng thời gian xác định (sử dụng native SQL).
    @Query(value = "SELECT CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) AS month, " +
            "COUNT(o.order_id) AS order_count, " +
            "SUM(CASE WHEN o.type = 'ORDER ONLINE' THEN 1 ELSE 0 END) AS online_orders, " +
            "SUM(CASE WHEN o.type = 'OFFLINE' THEN 1 ELSE 0 END) AS offline_orders " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) " +
            "ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyOrderCountByType(@Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);


    // Tính tổng doanh thu trong tháng hiện tại từ các đơn hàng "Đã thanh toán".
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    BigDecimal getTotalRevenueThisMonth();

    // Tính tổng doanh thu trong năm hiện tại từ các đơn hàng "Đã thanh toán".
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE)")
    BigDecimal getTotalRevenueThisYear();

    // Lấy doanh thu hàng ngày và số lượng đơn hàng trong một tháng cụ thể.
    @Query("SELECT DATE(o.orderDate) AS date, " +
            "COALESCE(SUM(o.totalAmount), 0) AS revenue, " +
            "COUNT(o) AS order_count " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND YEAR(o.orderDate) = :year " +
            "AND MONTH(o.orderDate) = :month " +
            "GROUP BY DATE(o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyRevenueByMonth(@Param("year") int year,
                                          @Param("month") int month);

    // Đếm tổng số đơn hàng trong tuần hiện tại với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND YEARWEEK(o.orderDate) = YEARWEEK(CURRENT_DATE)")
    Long getTotalOrdersThisWeek();

    // Đếm tổng số đơn hàng trong tháng hiện tại với trạng thái "Đã thanh toán".
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Long getTotalOrdersThisMonth();

    // Đếm tổng số đơn hàng của ngày hôm qua với trạng thái "Đã thanh toán" (sử dụng native SQL).
    @Query(value = "SELECT COUNT(*) " +
            "FROM orders " +
            "WHERE payment_status = 'Đã thanh toán' " +
            "AND DATE(order_date) = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)",
            nativeQuery = true)
    Long getTotalOrdersYesterday();

    // Đếm tổng số khách hàng duy nhất (dựa trên user) từ các đơn hàng "Đã thanh toán".
    @Query("SELECT COUNT(DISTINCT o.user) " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán'")
    Long getTotalCustomers();

    // Lấy danh sách 5 khách hàng có số lượng đơn hàng ORDER ONLINE nhiều nhất (sử dụng native SQL).
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

    // Lấy danh sách các voucher đã được áp dụng trong các đơn hàng "Đã thanh toán".
    @Query("SELECT DISTINCT v.voucherId, v.name, v.percents, v.condition, v.startDate, v.endDate, v.quantity, v.status " +
            "FROM Orders o " +
            "JOIN o.voucher v " +
            "WHERE o.voucher IS NOT NULL " +
            "AND o.paymentStatus = 'Đã thanh toán'")
    List<Object[]> getAppliedVouchers();

    // Tìm các đơn hàng sử dụng một voucher cụ thể dựa trên ID voucher.
    @Query("SELECT o FROM Orders o WHERE o.voucher.voucherId = :voucherId")
    List<Orders> findOrdersByVoucherId(@Param("voucherId") Long voucherId);

    // Tìm các đơn hàng OFFLINE trong khoảng thời gian xác định, sắp xếp theo ngày đặt hàng.
    @Query("SELECT o FROM Orders o " +
            "WHERE o.type = 'OFFLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderDate ASC")
    List<Orders> findOfflineOrdersByDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    // Kiểm tra xem một đơn hàng có tồn tại dựa trên ID đơn hàng.
    boolean existsByOrderId(Long orderId);

    // Tìm các đơn hàng dựa trên ID đơn hàng của Momo.
    List<Orders> findByMomoOrderId(String momoOrderId);

    // Tìm các đơn hàng theo phương thức thanh toán.
    List<Orders> findByPaymentMethod(String paymentMethod);

    // Lấy tổng doanh thu theo loại đơn hàng (ONLINE hoặc OFFLINE) trong khoảng thời gian xác định.
    @Query("SELECT o.type, COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY o.type")
    List<Object[]> getRevenueByTypeAndDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Lấy doanh thu hàng ngày theo loại đơn hàng (ONLINE hoặc OFFLINE) trong khoảng thời gian xác định.
    @Query("SELECT DATE(o.orderDate) as date, o.type, COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.orderDate), o.type")
    List<Object[]> getDailyRevenueByType(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Lấy tổng doanh thu từng tháng trong khoảng thời gian xác định (sử dụng native SQL).
    @Query(value = "SELECT CONCAT(YEAR(order_date), '-', LPAD(MONTH(order_date), 2, '0')) AS month, " +
            "COALESCE(SUM(total_amount), 0) AS revenue, " +
            "COUNT(order_id) AS order_count " +
            "FROM orders " +
            "WHERE payment_status = 'Đã thanh toán' " +
            "AND order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CONCAT(YEAR(order_date), '-', LPAD(MONTH(order_date), 2, '0')) " +
            "ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByDateRange(@Param("startDate") Date startDate,
                                                @Param("endDate") Date endDate);

    // Lấy tổng doanh thu từng tháng trong khoảng thời gian xác định theo loại đơn offline và online

    @Query(value = "SELECT CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) AS month, " +
            "SUM(CASE WHEN o.type = 'ORDER ONLINE' THEN o.total_amount ELSE 0 END) AS online_revenue, " +
            "SUM(CASE WHEN o.type = 'OFFLINE' THEN o.total_amount ELSE 0 END) AS offline_revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) " +
            "ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByOrderType(@Param("startDate") Date startDate,
                                                @Param("endDate") Date endDate);



    // Lấy doanh thu hàng tuần theo loại đơn hàng (ONLINE và OFFLINE) trong khoảng thời gian xác định (sử dụng native SQL).
    @Query(value = "SELECT YEARWEEK(o.order_date) AS week, o.type, COALESCE(SUM(o.total_amount), 0) AS revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.order_date), o.type " +
            "ORDER BY YEARWEEK(o.order_date), o.type",
            nativeQuery = true)
    List<Object[]> getWeeklyRevenueByTypeAndDateRange(@Param("startDate") Date startDate,
                                                      @Param("endDate") Date endDate);


    // Lấy top 5 sản phẩm được yêu thích nhất dựa trên số lượng lượt thích
    @Query(value = "SELECT p.product_id, p.product_name, p.image, COUNT(f.favorites_id) as favorite_count " +
            "FROM favorites f " +
            "JOIN products p ON f.product_id = p.product_id " +
            "WHERE f.is_liked = true " +
            "GROUP BY p.product_id, p.product_name, p.image " +
            "ORDER BY favorite_count DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> getTopFiveFavoriteProducts();


    // Đếm tổng số đơn hàng OFFLINE trong ngày hôm nay với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTotalOfflineOrdersTodayByCash();

    // Đếm tổng số đơn hàng OFFLINE trong ngày hôm nay với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTotalOfflineOrdersTodayByMomo();

    // Đếm tổng số đơn hàng OFFLINE trong tháng hiện tại với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Long getTotalOfflineOrdersThisMonthByCash();

    // Đếm tổng số đơn hàng OFFLINE trong tháng hiện tại với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Long getTotalOfflineOrdersThisMonthByMomo();

    // Đếm tổng số đơn hàng OFFLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOfflineOrdersByDateRangeAndCash(@Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate);

    // Đếm tổng số đơn hàng OFFLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COUNT(o) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long getTotalOfflineOrdersByDateRangeAndMomo(@Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate);

    // 2. Tính tổng doanh thu đơn hàng OFFLINE theo phương thức thanh toán

    // Tổng doanh thu đơn hàng OFFLINE trong ngày hôm nay với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    BigDecimal getOfflineRevenueTodayByCash();

    // Tổng doanh thu đơn hàng OFFLINE trong ngày hôm nay với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    BigDecimal getOfflineRevenueTodayByMomo();

    // Tổng doanh thu đơn hàng OFFLINE trong tháng hiện tại với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    BigDecimal getOfflineRevenueThisMonthByCash();

    // Tổng doanh thu đơn hàng OFFLINE trong tháng hiện tại với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    BigDecimal getOfflineRevenueThisMonthByMomo();

    // Tổng doanh thu đơn hàng OFFLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán" và phương thức thanh toán CASH
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'CASH' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getOfflineRevenueByDateRangeAndCash(@Param("startDate") Date startDate,
                                                   @Param("endDate") Date endDate);

    // Tổng doanh thu đơn hàng OFFLINE trong khoảng thời gian xác định với trạng thái "Đã thanh toán" và phương thức thanh toán MOMO
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.paymentMethod = 'MOMO' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal getOfflineRevenueByDateRangeAndMomo(@Param("startDate") Date startDate,
                                                   @Param("endDate") Date endDate);

    // 3. Cập nhật các truy vấn thống kê doanh thu hàng ngày, hàng tuần, hàng tháng để bao gồm payment_method

    // Lấy doanh thu hàng ngày đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong khoảng thời gian xác định
    @Query("SELECT DATE(o.orderDate) AS date, " +
            "SUM(CASE WHEN o.paymentMethod = 'CASH' THEN o.totalAmount ELSE 0 END) AS cash_revenue, " +
            "SUM(CASE WHEN o.paymentMethod = 'MOMO' THEN o.totalAmount ELSE 0 END) AS momo_revenue " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.orderDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyOfflineRevenueByPaymentMethod(@Param("startDate") Date startDate,
                                                         @Param("endDate") Date endDate);

    // Lấy doanh thu hàng tuần đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong khoảng thời gian xác định (sử dụng native SQL)
    @Query(value = "SELECT YEARWEEK(o.order_date) AS week, " +
            "SUM(CASE WHEN o.payment_method = 'CASH' THEN o.total_amount ELSE 0 END) AS cash_revenue, " +
            "SUM(CASE WHEN o.payment_method = 'MOMO' THEN o.total_amount ELSE 0 END) AS momo_revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY YEARWEEK(o.order_date) " +
            "ORDER BY week ASC",
            nativeQuery = true)
    List<Object[]> getWeeklyOfflineRevenueByPaymentMethod(@Param("startDate") Date startDate,
                                                          @Param("endDate") Date endDate);

    // Lấy doanh thu hàng tháng đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong khoảng thời gian xác định (sử dụng native SQL)
    @Query(value = "SELECT CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) AS month, " +
            "SUM(CASE WHEN o.payment_method = 'CASH' THEN o.total_amount ELSE 0 END) AS cash_revenue, " +
            "SUM(CASE WHEN o.payment_method = 'MOMO' THEN o.total_amount ELSE 0 END) AS momo_revenue " +
            "FROM orders o " +
            "WHERE o.payment_status = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) " +
            "ORDER BY month",
            nativeQuery = true)
    List<Object[]> getMonthlyOfflineRevenueByPaymentMethod(@Param("startDate") Date startDate,
                                                           @Param("endDate") Date endDate);

    // 4. Đếm số lượng đơn hàng OFFLINE theo phương thức thanh toán

    // Đếm số lượng đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong ngày hôm nay
    @Query("SELECT SUM(CASE WHEN o.paymentMethod = 'CASH' THEN 1 ELSE 0 END) AS cash_orders, " +
            "SUM(CASE WHEN o.paymentMethod = 'MOMO' THEN 1 ELSE 0 END) AS momo_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Object[] getOfflineOrderCountTodayByPaymentMethod();

    // Đếm số lượng đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong tháng hiện tại
    @Query("SELECT SUM(CASE WHEN o.paymentMethod = 'CASH' THEN 1 ELSE 0 END) AS cash_orders, " +
            "SUM(CASE WHEN o.paymentMethod = 'MOMO' THEN 1 ELSE 0 END) AS momo_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    Object[] getOfflineOrderCountThisMonthByPaymentMethod();

    // Đếm số lượng đơn hàng OFFLINE theo phương thức thanh toán (CASH và MOMO) trong khoảng thời gian xác định
    @Query("SELECT SUM(CASE WHEN o.paymentMethod = 'CASH' THEN 1 ELSE 0 END) AS cash_orders, " +
            "SUM(CASE WHEN o.paymentMethod = 'MOMO' THEN 1 ELSE 0 END) AS momo_orders " +
            "FROM Orders o " +
            "WHERE o.paymentStatus = 'Đã thanh toán' " +
            "AND o.type = 'OFFLINE' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Object[] getOfflineOrderCountByDateRangeAndPaymentMethod(@Param("startDate") Date startDate,
                                                             @Param("endDate") Date endDate);
}