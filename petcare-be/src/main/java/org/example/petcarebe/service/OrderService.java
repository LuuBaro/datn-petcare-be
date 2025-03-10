package org.example.petcarebe.service;

import org.example.petcarebe.controller.WebSocketController;
import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.OrderDetailDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.dto.request.OrderItemDTO;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.sql.Timestamp;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    @Autowired
    private StatusOrderRepository statusOrderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CartDetailsService cartDetailsService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public List<Map<String, Object>> getBestSellingProducts() {
        // Lấy 5 sản phẩm bán chạy nhất
        Pageable topFive = PageRequest.of(0, 5); // Lấy 5 sản phẩm đầu
        List<Object[]> results = orderDetailsRepository.findBestSellingProducts(topFive);

        List<Map<String, Object>> bestSellingProducts = new ArrayList<>();

        for (Object[] result : results) {
            ProductDetails product = (ProductDetails) result[0];
            Long totalSold = (Long) result[1];

            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("productId", product.getProducts().getProductId());
            productInfo.put("productDetailId", product.getProductDetailId());
            productInfo.put("productName", product.getProducts().getProductName());
            productInfo.put("price", product.getPrice());
            productInfo.put("colorValue", product.getProductColors().getColorValue());
            productInfo.put("sizeValue", product.getProductSizes().getSizeValue());
            productInfo.put("weightValue", product.getWeights().getWeightValue());
            productInfo.put("image", product.getProducts().getImage());
            productInfo.put("totalSold", totalSold);

            bestSellingProducts.add(productInfo);
        }

        return bestSellingProducts;
    }

    public void clearCartDetailsByUserId(Long cartDetailId) {
        cartDetailsService.deleteCartDetails(cartDetailId);
    }

    @Transactional
    public Orders checkout(CheckoutRequestDTO request) {
        // 1️⃣ Kiểm tra người dùng
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        if (request.getShippingAddress() == null || request.getShippingAddress().isEmpty()) {
            throw new RuntimeException("Địa chỉ giao hàng không được bỏ trống!");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            throw new RuntimeException("Phương thức thanh toán không được bỏ trống!");
        }

        // 2️⃣ Tạo đơn hàng
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCost(request.getShippingCost());
        order.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : "Chờ thanh toán");
        order.setStatusOrder(statusOrderRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Trạng thái đơn hàng không hợp lệ!")));
        order.setType(request.getType());
        order.setPointEarned(0);
        order.setPointUsed(0);

        // 3️⃣ Kiểm tra voucher
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ!"));
            order.setVoucher(voucher);
        }

        // 4️⃣ Thêm chi tiết đơn hàng và kiểm tra số lượng tồn kho
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Long> outOfStockItems = new ArrayList<>();

        for (OrderItemDTO item : request.getItems()) {
            ProductDetails product = productDetailsRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + item.getProductDetailId()));

            if (product.getQuantity() < item.getQuantity()) {
                outOfStockItems.add(item.getProductDetailId());
                continue;
            }

            OrderDetails orderDetail = new OrderDetails();
            orderDetail.setOrders(order);
            orderDetail.setProductDetails(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(item.getPrice());

            orderDetailsList.add(orderDetail);
            totalAmount = totalAmount.add(
                    BigDecimal.valueOf(item.getQuantity()).multiply(BigDecimal.valueOf(item.getPrice()))
            );
        }

        if (!outOfStockItems.isEmpty()) {
            for (Long productId : outOfStockItems) {
                cartDetailsService.removeProductFromCart(productId);
            }
            throw new RuntimeException("Một số sản phẩm đã hết hàng!");
        }

        if (orderDetailsList.isEmpty()) {
            throw new RuntimeException("Giỏ hàng rỗng hoặc tất cả sản phẩm đều hết hàng!");
        }

        // 5️⃣ Cập nhật tổng tiền
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ!"));
            BigDecimal percentDiscount = BigDecimal.valueOf(voucher.getPercents()).divide(BigDecimal.valueOf(100));
            discountAmount = totalAmount.add(BigDecimal.valueOf(order.getShippingCost())).multiply(percentDiscount);
        }

        BigDecimal finalAmount = totalAmount.add(BigDecimal.valueOf(order.getShippingCost())).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        order.setTotalAmount(finalAmount.floatValue());
        order.setOrderDetails(orderDetailsList);

        // 6️⃣ Lưu đơn hàng
        Orders savedOrder = orderRepository.save(order);

        // 7️⃣ Trừ kho và clear giỏ hàng ngay lập tức cho COD
        if ("COD".equals(request.getPaymentMethod())) {
            for (OrderDetails orderDetail : savedOrder.getOrderDetails()) {
                int updated = productDetailsRepository.updateStock(
                        orderDetail.getProductDetails().getProductDetailId(),
                        orderDetail.getQuantity()
                );
                if (updated == 0) {
                    throw new RuntimeException("Không thể cập nhật tồn kho cho sản phẩm: " +
                            orderDetail.getProductDetails().getProductDetailId());
                }
            }
            cartDetailsService.clearCartDetailsByUserId(request.getUserId());
            logger.info("Stock deducted and cart cleared for COD orderId: {}", savedOrder.getOrderId());
        }
        // ❌ Không trừ kho cho VNPay ở đây, chỉ trừ khi thanh toán thành công

        return savedOrder;
    }

    public Orders updatePaymentStatus(Long orderId, String paymentStatus) {
        logger.info("Received request to update paymentStatus for orderId: {} to {}", orderId, paymentStatus);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String oldPaymentStatus = order.getPaymentStatus();
        logger.info("Current paymentStatus for orderId: {} is {}", orderId, oldPaymentStatus);

        // Cập nhật trạng thái thanh toán
        order.setPaymentStatus(paymentStatus);

        // Hủy đơn hàng nếu VNPay bị hủy thanh toán
        if ("VNPay".equals(order.getPaymentMethod()) && "Đã hủy thanh toán".equals(paymentStatus)) {
            StatusOrder cancelledStatus = statusOrderRepository.findById(5L)
                    .orElseThrow(() -> new RuntimeException("Status 'Cancelled' not found"));
            order.setStatusOrder(cancelledStatus);
            logger.info("Order {} cancelled due to VNPay payment cancellation", orderId);
        }

        // Lưu trạng thái đơn hàng trước
        Orders savedOrder = orderRepository.save(order);
        logger.info("Updated paymentStatus for orderId: {} to {}", orderId, paymentStatus);

        // ✅ Chỉ trừ kho khi VNPay chuyển từ "Chờ thanh toán" sang "Chờ xác nhận"
        if ("VNPay".equals(order.getPaymentMethod()) && "Chờ xác nhận".equals(paymentStatus) && "Chờ thanh toán".equals(oldPaymentStatus)) {
            logger.info("Deducting stock for VNPay orderId: {}", orderId);
            for (OrderDetails orderDetail : savedOrder.getOrderDetails()) {
                    int updated = productDetailsRepository.updateStock(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()
                    );
                    logger.info("Stock updated for product {}: rows affected {}",
                            orderDetail.getProductDetails().getProductDetailId(), updated);
                    if (updated == 0) {
                        logger.error("Failed to update stock for product: {}",
                                orderDetail.getProductDetails().getProductDetailId());
                        throw new RuntimeException("Không thể cập nhật tồn kho cho sản phẩm: " +
                                orderDetail.getProductDetails().getProductDetailId());
                    }
            }
            logger.info("Clearing cart for VNPay order, userId: {}", order.getUser().getUserId());
            cartDetailsService.clearCartDetailsByUserId(order.getUser().getUserId());
        } else {
            logger.info("No stock deduction for orderId: {} - condition not met (paymentMethod: {}, oldStatus: {}, newStatus: {})",
                    orderId, order.getPaymentMethod(), oldPaymentStatus, paymentStatus);
        }

        return savedOrder;
    }

    // Lấy tất cả đơn hàng
    public List<OrderDTO> getAllOrders() {
        List<Orders> ordersList = orderRepository.findAll();
        return ordersList.stream().map(this::convertToOrderDTO).collect(Collectors.toList());
    }

    // Chuyển từ Orders sang OrderDTO
    private OrderDTO convertToOrderDTO(Orders order) {
        List<OrderDetailDTO> orderDetailDTOList = order.getOrderDetails().stream().map(this::convertToOrderDetailDTO).collect(Collectors.toList());



        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(order.getShippingAddress())
                .shippingCost(order.getShippingCost())
                .totalAmount(order.getTotalAmount())
                .type(order.getType())
                .pointEarned(order.getPointEarned())
                .pointUsed(order.getPointUsed())
                .userId(order.getUser().getUserId())
                .userName(order.getUser().getFullName())
                .phone(order.getUser().getPhone()) // Thêm số điện thoại từ User
                .statusId(order.getStatusOrder() != null ? order.getStatusOrder().getStatusId() : null)
                .statusName(order.getStatusOrder() != null ? order.getStatusOrder().getStatusName() : null)
                .voucherId(order.getVoucher() != null ? order.getVoucher().getVoucherId() : null)
                .orderDetails(orderDetailDTOList)
                .build();
    }

    // Chuyển từ OrderDetails sang OrderDetailDTO
    private OrderDetailDTO convertToOrderDetailDTO(OrderDetails orderDetails) {
        return OrderDetailDTO.builder()
                .orderDetailId(orderDetails.getOrderDetailsId())
                .quantity(orderDetails.getQuantity())
                .price(orderDetails.getPrice())
                .productDetailId(orderDetails.getProductDetails().getProductDetailId())
                .productName(orderDetails.getProductDetails().getProducts().getProductName())
                .imageUrl(orderDetails.getProductDetails().getProducts().getImage()) // Lấy ảnh sản phẩm
                .colorValue(orderDetails.getProductDetails().getProductColors().getColorValue()) // Lấy màu sắc
                .sizeValue(orderDetails.getProductDetails().getProductSizes().getSizeValue()) // Lấy kích thước
                .weightValue(orderDetails.getProductDetails().getWeights().getWeightValue()) // Lấy trọng lượng
                .build();
    }

    @Transactional
    public Orders cancelOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getStatusOrder().getStatusId().equals(1L)) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái chờ.");
        }

        StatusOrder cancelledStatus = statusOrderRepository.findById(5L)
                .orElseThrow(() -> new RuntimeException("Status 'Cancelled' not found"));
        order.setStatusOrder(cancelledStatus);

        // Hoàn kho nếu kho đã được trừ (dựa trên paymentStatus)
        if ("COD".equals(order.getPaymentMethod()) ||
                ("VNPay".equals(order.getPaymentMethod()) && !"Chờ thanh toán".equals(order.getPaymentStatus()))) {
            for (OrderDetails orderDetail : order.getOrderDetails()) {
                int updated = productDetailsRepository.updateStockcancel(
                        orderDetail.getProductDetails().getProductDetailId(),
                        orderDetail.getQuantity()
                );
                if (updated == 0) {
                    throw new RuntimeException("Cập nhật tồn kho thất bại cho sản phẩm: " +
                            orderDetail.getProductDetails().getProductDetailId());
                }
            }
            logger.info("Stock restored for cancelled orderId: {}", orderId);
        }

        return orderRepository.save(order);
    }

    // Thống kê

    public BigDecimal getRevenueByDateRange(Date startDate, Date endDate) {
        return orderRepository.getTotalRevenueByDateRange(startDate, endDate);
    }

    public Map<Date, Map<String, Object>> getDailyRevenueByDateRange(Date startDate, Date endDate) {
        Calendar cal = Calendar.getInstance();

        // Đặt startDate về 00:00:00
        cal.setTime(startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDateInclusive = cal.getTime();

        // Đặt endDate về 23:59:59
        cal.setTime(endDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date endDateInclusive = cal.getTime();

        // Truy vấn dữ liệu
        List<Object[]> results = orderRepository.getDailyRevenueByDateRange(startDateInclusive, endDateInclusive);
        Map<Date, Map<String, Object>> dailyStats = new LinkedHashMap<>();

        for (Object[] row : results) {
            Date date = (Date) row[0]; // Ngày
            BigDecimal revenue = new BigDecimal(row[1].toString()); // Tổng doanh thu
            Long orderCount = (Long) row[2]; // Số đơn hàng

            Map<String, Object> stats = new HashMap<>();
            stats.put("revenue", revenue);
            stats.put("orderCount", orderCount);
            dailyStats.put(date, stats);
        }
        return dailyStats;
    }

    public Map<Date, Map<String, Object>> getDailyRevenueByMonth(int year, int month) {
        List<Object[]> results = orderRepository.getDailyRevenueByMonth(year, month);
        Map<Date, Map<String, Object>> dailyStats = new LinkedHashMap<>();

        for (Object[] row : results) {
            Date date = (Date) row[0];
            BigDecimal revenue = new BigDecimal(row[1].toString());
            Long orderCount = (Long) row[2];

            Map<String, Object> stats = new HashMap<>();
            stats.put("revenue", revenue);
            stats.put("orderCount", orderCount);

            dailyStats.put(date, stats);
        }
        return dailyStats;
    }

    public List<Map<String, Object>> getWeeklyRevenueByDateRange(Date startDate, Date endDate) {
        List<Object[]> results = orderRepository.getWeeklyRevenueByDateRange(startDate, endDate);
        List<Map<String, Object>> revenueList = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> revenueMap = new HashMap<>();
            revenueMap.put("week", row[0]);
            revenueMap.put("revenue", new BigDecimal(row[1].toString()));
            revenueList.add(revenueMap);
        }
        return revenueList;
    }

    public BigDecimal getRevenueThisMonth() {
        return orderRepository.getTotalRevenueThisMonth();
    }

    public BigDecimal getRevenueThisYear() {
        return orderRepository.getTotalRevenueThisYear();
    }

    public BigDecimal getRevenueToday() {
        return orderRepository.getRevenueToday();
    }

    public BigDecimal getRevenueYesterday() {
        return orderRepository.getRevenueYesterday();
    }

    // Tổng số đơn hàng OFFLINE hôm qua
    public Long getTotalOfflineOrdersYesterday() {
        return orderRepository.getTotalOfflineOrdersYesterday();
    }

    // Tổng số đơn hàng ONLINE hôm qua
    public Long getTotalOnlineOrdersYesterday() {
        return orderRepository.getTotalOnlineOrdersYesterday();
    }

    public Map<Date, Map<String, Long>> getDailyOrderCountByType(Date startDate, Date endDate) {
        List<Object[]> results = orderRepository.getDailyOrderCountByType(startDate, endDate);
        Map<Date, Map<String, Long>> dailyOrderStats = new LinkedHashMap<>();

        for (Object[] row : results) {
            Date date = (Date) row[0];
            Long onlineOrders = (Long) row[1];
            Long offlineOrders = (Long) row[2];

            Map<String, Long> stats = new HashMap<>();
            stats.put("onlineOrders", onlineOrders);
            stats.put("offlineOrders", offlineOrders);

            dailyOrderStats.put(date, stats);
        }
        return dailyOrderStats;
    }



    // Tổng số đơn hàng trong ngày hôm nay
    public Long getTotalOrdersToday() {
        return orderRepository.getTotalOrdersToday();
    }

    // Tổng số đơn hàng OFFLINE hôm nay
    public Long getTotalOfflineOrdersToday() {
        return orderRepository.getTotalOfflineOrdersToday();
    }

    // Tổng số đơn hàng ORDER ONLINE hôm nay
    public Long getTotalOnlineOrdersToday() {
        return orderRepository.getTotalOnlineOrdersToday();
    }

    // Tổng số đơn hàng trong tuần này
    public Long getTotalOrdersThisWeek() {
        return orderRepository.getTotalOrdersThisWeek();
    }

    // Tổng số đơn hàng trong tháng này
    public Long getTotalOrdersThisMonth() {
        return orderRepository.getTotalOrdersThisMonth();
    }

    // Tổng số đơn hàng OFFLINE trong tháng này
    public Long getTotalOfflineOrdersThisMonth() {
        return orderRepository.getTotalOfflineOrdersThisMonth();
    }

    // Tổng số đơn hàng ORDER ONLINE trong tháng này
    public Long getTotalOnlineOrdersThisMonth() {
        return orderRepository.getTotalOnlineOrdersThisMonth();
    }

    // Tổng số đơn hàng hôm qua
    public Long getTotalOrdersYesterday() {
        return orderRepository.getTotalOrdersYesterday();
    }

    // Tổng số đơn hàng OFFLINE trong khoảng thời gian
    public Long getTotalOfflineOrdersByDateRange(Date startDate, Date endDate) {
        return orderRepository.getTotalOfflineOrdersByDateRange(startDate, endDate);
    }

    // Tổng số đơn hàng ORDER ONLINE trong khoảng thời gian
    public Long getTotalOnlineOrdersByDateRange(Date startDate, Date endDate) {
        return orderRepository.getTotalOnlineOrdersByDateRange(startDate, endDate);
    }

    // Tổng số khách hàng
    public Long getTotalCustomers() {
        return orderRepository.getTotalCustomers();
    }

    public List<Map<String, Object>> getTopFiveCustomersByOrderCount() {
        List<Object[]> results = orderRepository.getTopFiveCustomersByOrderCount();
        List<Map<String, Object>> topCustomers = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("userId", row[0]); // Long
            customerInfo.put("fullName", row[1]); // String
            customerInfo.put("phone", row[2]); // Ép kiểu thành String để tránh lỗi
            customerInfo.put("orderCount", row[3]); // Long
            topCustomers.add(customerInfo);
        }

        return topCustomers;
    }
    public List<Map<String, Object>> getWeeklyOrderCountByType(Date startDate, Date endDate) {
        List<Object[]> results = orderRepository.getWeeklyOrderCountByType(startDate, endDate);
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("week", row[0]); // YEARWEEK (e.g., 202510)
            orderMap.put("orderCount", row[1]); // Tổng đơn hàng
            orderMap.put("onlineOrders", row[2]); // Đơn online
            orderMap.put("offlineOrders", row[3]); // Đơn offline
            orderList.add(orderMap);
        }
        return orderList;
    }

    public List<Map<String, Object>> getMonthlyOrderCountByType(Date startDate, Date endDate) {
        List<Object[]> results = orderRepository.getMonthlyOrderCountByType(startDate, endDate);
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("month", row[0]); // String: yyyy-MM
            orderMap.put("orderCount", row[1]); // Long: tổng số đơn hàng
            orderMap.put("onlineOrders", row[2]); // Long: số đơn online
            orderMap.put("offlineOrders", row[3]); // Long: số đơn offline
            orderList.add(orderMap);
        }
        return orderList;
    }

////



    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Orders> userOrders = orderRepository.findByUserUserId(userId);
        return userOrders.stream().map(this::convertToOrderDTO).collect(Collectors.toList());
    }

    @Transactional
    public Orders updateOrderStatus(Long orderId, Long statusId) {
        // 1️⃣ Tìm đơn hàng theo orderId
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // 2️⃣ Kiểm tra trạng thái đơn hàng hiện tại
        if (order.getStatusOrder() == null) {
            throw new RuntimeException("Đơn hàng không có trạng thái hợp lệ!");
        }
        Long currentStatusId = order.getStatusOrder().getStatusId();

        // 3️⃣ Chặn cập nhật nếu trạng thái hiện tại là Hoàn thành, Đã hủy, hoặc Trả hàng
        List<Long> finalStatuses = Arrays.asList(4L, 5L, 6L);
        if (finalStatuses.contains(currentStatusId)) {
            throw new RuntimeException("Không thể cập nhật trạng thái từ 'Hoàn thành', 'Đã hủy' hoặc 'Trả hàng'.");
        }

        // 4️⃣ Kiểm tra statusId có hợp lệ không
        List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        if (!validStatusIds.contains(statusId)) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ!");
        }

        // 5️⃣ Tìm trạng thái mới theo statusId
        StatusOrder newStatus = statusOrderRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Trạng thái đơn hàng không hợp lệ!"));

        // 6️⃣ Nếu trạng thái mới là "Hoàn thành" (statusId = 4), cập nhật paymentStatus
        if (statusId.equals(4L)) {
            order.setPaymentStatus("Đã thanh toán");
        }

        // 7️⃣ Nếu cập nhật trạng thái sang hủy/trả hàng, hoàn lại số lượng tồn kho
        if (statusId.equals(5L) || statusId.equals(6L)) {
            for (OrderDetails orderDetail : order.getOrderDetails()) {
                if (orderDetail.getProductDetails() != null) {
                    productDetailsRepository.updateStockcancel(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()
                    );
                }
            }
        }

        // 8️⃣ Cập nhật trạng thái mới
        order.setStatusOrder(newStatus);

        // 9️⃣ Lưu đơn hàng đã cập nhật
        Orders savedOrder = orderRepository.save(order);

        // 10️⃣ Kiểm tra lại trạng thái đã được cập nhật chưa
        if (!savedOrder.getStatusOrder().getStatusId().equals(statusId)) {
            throw new RuntimeException("Lỗi cập nhật trạng thái đơn hàng!");
        }

        // 11️⃣ Lưu và gửi thông báo qua WebSocket
        Long userId = order.getUser().getUserId();
        String message = "Đơn hàng #" + orderId + " của bạn đã được cập nhật thành trạng thái: " + newStatus.getStatusName();
        notificationService.saveNotification(userId, message); // Lưu thông báo vào database

        try {
            webSocketService.sendToTopic("/topic/status", message); // Gửi broadcast
            System.out.println("✅ WebSocket notification broadcast to /topic/status: " + message);
        } catch (Exception e) {
            System.err.println("❌ Failed to send WebSocket notification to /topic/status: " + e.getMessage());
            // Có thể ghi log, nhưng không làm gián đoạn luồng chính
        }
        return savedOrder;
    }
}
