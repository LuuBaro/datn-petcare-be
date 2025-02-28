package org.example.petcarebe.service;

import org.example.petcarebe.controller.WebSocketController;
import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.OrderDetailDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.dto.request.OrderItemDTO;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.sql.Timestamp;


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

    public Orders updatePaymentStatus(Long orderId, String paymentStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setPaymentStatus(paymentStatus);
        Orders updatedOrder = orderRepository.save(order);

        // Clear cart if payment is confirmed
        if ("Chờ xác nhận".equals(paymentStatus) || "Đã thanh toán".equals(paymentStatus)) {
            cartDetailsService.clearCartDetailsByUserId(order.getUser().getUserId());
        }

        return updatedOrder;
    }

    @Transactional
    public Orders checkout(CheckoutRequestDTO request) {
        // 1️⃣ Kiểm tra người dùng
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        // Kiểm tra thông tin bắt buộc
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

        // 4️⃣ Thêm chi tiết đơn hàng và cập nhật số lượng tồn kho
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

            // Chỉ cập nhật tồn kho khi đã thanh toán
            if ("Đã thanh toán".equals(request.getPaymentStatus())) {
                int updated = productDetailsRepository.updateStock(item.getProductDetailId(), item.getQuantity());
                if (updated == 0) {
                    outOfStockItems.add(item.getProductDetailId());
                }
            }

            if ("Đã thanh toán".equals(request.getPaymentStatus())) {
                cartDetailsService.clearCartDetailsByUserId(request.getUserId());
            }
        }

        // Xóa sản phẩm hết hàng khỏi giỏ hàng
        for (Long productId : outOfStockItems) {
            cartDetailsService.removeProductFromCart(productId);
        }

        if (orderDetailsList.isEmpty()) {
            throw new RuntimeException("Tất cả sản phẩm trong giỏ hàng đều đã hết hàng!");
        }

        // 5️⃣ Cập nhật tổng tiền
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ!"));
            BigDecimal percentDiscount = BigDecimal.valueOf(voucher.getPercents()).divide(BigDecimal.valueOf(100));
            discountAmount = (totalAmount.add(BigDecimal.valueOf(order.getShippingCost()))).multiply(percentDiscount);
        }

        BigDecimal finalAmount = totalAmount.add(BigDecimal.valueOf(order.getShippingCost())).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        order.setTotalAmount(finalAmount.floatValue());

        order.setOrderDetails(orderDetailsList);

        // 6️⃣ Lưu đơn hàng
        Orders savedOrder = orderRepository.save(order);

        // 7️⃣ Xóa giỏ hàng chỉ khi đã thanh toán
        if ("Chờ xác nhận".equals(request.getPaymentStatus()) || "Đã thanh toán".equals(request.getPaymentStatus())) {
            cartDetailsService.clearCartDetailsByUserId(request.getUserId());
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
        // 1️⃣ Tìm đơn hàng
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // 2️⃣ Kiểm tra trạng thái đơn hàng (chỉ hủy nếu trạng thái là "PENDING")
        if (!order.getStatusOrder().getStatusId().equals(1L)) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái chờ.");
        }

        // 3️⃣ Cập nhật trạng thái đơn hàng thành "CANCELLED" (statusId = 5)
        StatusOrder cancelledStatus = statusOrderRepository.findById(5L)
                .orElseThrow(() -> new RuntimeException("Status 'Cancelled' not found"));
        order.setStatusOrder(cancelledStatus);

        // 4️⃣ Hoàn lại số lượng tồn kho (Sử dụng Native Query để tránh lỗi khóa ngoại)
        for (OrderDetails orderDetail : order.getOrderDetails()) {
            int updated = productDetailsRepository.updateStockcancel(
                    orderDetail.getProductDetails().getProductDetailId(),
                    orderDetail.getQuantity()
            );
            if (updated == 0) {
                throw new RuntimeException("Cập nhật tồn kho thất bại cho sản phẩm: " + orderDetail.getProductDetails().getProductDetailId());
            }
        }

        // 5️⃣ Lưu đơn hàng đã hủy vào database
        return orderRepository.save(order);


    }

    public BigDecimal getRevenueByDateRange(Date startDate, Date endDate) {
        return orderRepository.getTotalRevenueByDateRange(startDate, endDate);
    }

    public Map<Date, Map<String, Object>> getDailyRevenueByDateRange(Date startDate, Date endDate) {
        List<Object[]> results = orderRepository.getDailyRevenueByDateRange(startDate, endDate);
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
        System.out.println("Query Results: " + results);

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

    // Tổng số đơn hàng trong ngày hôm nay
    public Long getTotalOrdersToday() {
        return orderRepository.getTotalOrdersToday();
    }

    // Tổng số đơn hàng trong tuần này
    public Long getTotalOrdersThisWeek() {
        return orderRepository.getTotalOrdersThisWeek();
    }

    // Tổng số đơn hàng trong tháng này
    public Long getTotalOrdersThisMonth() {
        return orderRepository.getTotalOrdersThisMonth();
    }

    // Tổng số đơn hàng hôm qua
    public Long getTotalOrdersYesterday() {
        return orderRepository.getTotalOrdersYesterday();
    }

    //  Tổng số khách hàng
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

        // 3️⃣ Kiểm tra statusId có hợp lệ không
        List<Long> validStatusIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        if (!validStatusIds.contains(statusId)) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ!");
        }

        // ✅ Cho phép cập nhật trạng thái, kể cả khi đơn hàng đã bị hủy/trả hàng
        // => Xóa điều kiện chặn cập nhật khi trạng thái là 5 hoặc 6

        // 4️⃣ Tìm trạng thái mới theo statusId
        StatusOrder newStatus = statusOrderRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Trạng thái đơn hàng không hợp lệ!"));

        // 5️⃣ Nếu cập nhật trạng thái sang hủy/trả hàng, hoàn lại số lượng tồn kho
        if (statusId.equals(5L) || statusId.equals(6L)) {
            for (OrderDetails orderDetail : order.getOrderDetails()) {
                if (orderDetail.getProductDetails() != null) {
                    productDetailsRepository.updateStockcancel(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()

                    );
                    System.out.println("Updating order " + orderId + " to status " + statusId);
                }
            }
        }

        // 6️⃣ Cập nhật trạng thái mới
        order.setStatusOrder(newStatus);

        // 7️⃣ Lưu đơn hàng đã cập nhật
        Orders savedOrder = orderRepository.save(order);

        // 8️⃣ Kiểm tra lại trạng thái đã được cập nhật chưa
        if (!savedOrder.getStatusOrder().getStatusId().equals(statusId)) {
            throw new RuntimeException("Lỗi cập nhật trạng thái đơn hàng!");
        }

        // 9️⃣ Gửi thông báo qua WebSocket đến người dùng
        // Gửi thông báo đến user
        Long userId = order.getUser().getUserId();
        String message = "Đơn hàng #" + orderId + " của bạn đã được cập nhật thành trạng thái: " + newStatus.getStatusName();
        webSocketService.sendToUser(userId, "/queue/notifications", message);

        return savedOrder;
    }
}
