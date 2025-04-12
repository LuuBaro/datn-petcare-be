package org.example.petcarebe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private JavaMailSender mailSender;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

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
        
        // Lưu momoOrderId nếu có
        if (request.getMomoOrderId() != null && !request.getMomoOrderId().isEmpty()) {
            order.setMomoOrderId(request.getMomoOrderId());
        }
        
        // Lưu momoTransId nếu có
        if (request.getMomoTransId() != null && !request.getMomoTransId().isEmpty()) {
            order.setMomoTransId(request.getMomoTransId());
        }
        
        // Lưu momoAmount nếu có
        if (request.getMomoAmount() != null && !request.getMomoAmount().isEmpty()) {
            order.setMomoAmount(request.getMomoAmount());
        }

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
        // Trừ kho và clear giỏ hàng cho VNPay/MoMo khi trạng thái là "Chờ xác nhận"
        else if (("VNPay".equals(request.getPaymentMethod()) || "MoMo".equals(request.getPaymentMethod())) 
                 && "Chờ xác nhận".equals(request.getPaymentStatus())) {
            logger.info("Processing inventory and cart for VNPay/MoMo order with 'Chờ xác nhận' status, orderId: {}", savedOrder.getOrderId());
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
            logger.info("Stock deducted and cart cleared for VNPay/MoMo orderId: {}", savedOrder.getOrderId());
        }
        // ❌ Không trừ kho cho VNPay/MoMo với trạng thái khác, chỉ trừ khi thanh toán thành công

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

        // Hủy đơn hàng nếu VNPay/MoMo bị hủy thanh toán hoặc hoàn tiền
        if (("VNPay".equals(order.getPaymentMethod()) || "MoMo".equals(order.getPaymentMethod())) && 
            ("Đã hủy thanh toán".equals(paymentStatus) || "Đã hoàn tiền".equals(paymentStatus))) {
            StatusOrder cancelledStatus = statusOrderRepository.findById(5L)
                    .orElseThrow(() -> new RuntimeException("Status 'Cancelled' not found"));
            order.setStatusOrder(cancelledStatus);
            logger.info("Order {} cancelled due to payment cancellation/refund", orderId);
            
            // Ghi log thêm thông tin chi tiết
            if ("Đã hoàn tiền".equals(paymentStatus)) {
                logger.info("Order {} marked as refunded for payment method: {}", orderId, order.getPaymentMethod());
            }
        }

        // Lưu trạng thái đơn hàng trước
        Orders savedOrder = orderRepository.save(order);
        logger.info("Updated paymentStatus for orderId: {} from '{}' to '{}'", orderId, oldPaymentStatus, paymentStatus);

        // ✅ Chỉ trừ kho khi VNPay/MoMo chuyển từ "Chờ thanh toán" sang "Chờ xác nhận"
        if ((("VNPay".equals(order.getPaymentMethod()) || "MoMo".equals(order.getPaymentMethod())) 
                && "Chờ xác nhận".equals(paymentStatus) && "Chờ thanh toán".equals(oldPaymentStatus))) {
            logger.info("Deducting stock for online payment orderId: {}", orderId);
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
            logger.info("Clearing cart for online payment order, userId: {}", order.getUser().getUserId());
            cartDetailsService.clearCartDetailsByUserId(order.getUser().getUserId());
        } 
        // ✅ Hoàn trả stock khi đơn hàng bị hủy hoặc hoàn tiền
        else if (("VNPay".equals(order.getPaymentMethod()) || "MoMo".equals(order.getPaymentMethod())) 
                && ("Đã hủy thanh toán".equals(paymentStatus) || "Đã hoàn tiền".equals(paymentStatus)) 
                && !"Đã hủy thanh toán".equals(oldPaymentStatus) 
                && !"Đã hoàn tiền".equals(oldPaymentStatus)) {
            logger.info("Restoring stock for cancelled/refunded online payment orderId: {}", orderId);
            for (OrderDetails orderDetail : savedOrder.getOrderDetails()) {
                try {
                    int updated = productDetailsRepository.updateStockcancel(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()
                    );
                    logger.info("Stock restored for product {}: rows affected {}",
                            orderDetail.getProductDetails().getProductDetailId(), updated);
                    if (updated == 0) {
                        logger.error("Failed to restore stock for product: {}",
                                orderDetail.getProductDetails().getProductDetailId());
                        // Không throw exception ở đây để tiếp tục xử lý các sản phẩm khác
                    }
                } catch (Exception e) {
                    logger.error("Error restoring stock for product {}: {}",
                            orderDetail.getProductDetails().getProductDetailId(), e.getMessage());
                }
            }
        } else {
            logger.info("No stock changes for orderId: {} - condition not met (paymentMethod: {}, oldStatus: {}, newStatus: {})",
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
    public Orders cancelOrder(Long orderId, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getStatusOrder().getStatusId().equals(1L)) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái chờ.");
        }

        StatusOrder cancelledStatus = statusOrderRepository.findById(5L)
                .orElseThrow(() -> new RuntimeException("Status 'Cancelled' not found"));
        order.setStatusOrder(cancelledStatus);

        // Luôn khôi phục tồn kho khi hủy đơn hàng, bất kể phương thức thanh toán và trạng thái
        try {
            for (OrderDetails orderDetail : order.getOrderDetails()) {
                int updated = productDetailsRepository.updateStockcancel(
                        orderDetail.getProductDetails().getProductDetailId(),
                        orderDetail.getQuantity()
                );
                if (updated == 0) {
                    logger.error("Cập nhật tồn kho thất bại cho sản phẩm: {}", 
                            orderDetail.getProductDetails().getProductDetailId());
                    // Không throw exception, tiếp tục quá trình hủy đơn và các mặt hàng khác
                } else {
                    logger.info("Đã khôi phục {} sản phẩm {} vào kho", 
                            orderDetail.getQuantity(), 
                            orderDetail.getProductDetails().getProductDetailId());
                }
            }
            logger.info("Đã khôi phục tồn kho cho đơn hàng bị hủy, orderId: {}", orderId);
        } catch (Exception e) {
            logger.error("Lỗi khi khôi phục tồn kho cho đơn hàng {}: {}", orderId, e.getMessage());
            // Vẫn tiếp tục quá trình hủy đơn
        }

        Orders savedOrder = orderRepository.save(order);
        
        // Cập nhật trạng thái thanh toán thành "Đã hoàn tiền" nếu là MoMo hoặc VNPay
        if ("MoMo".equals(order.getPaymentMethod()) || "VNPay".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("Đã hoàn tiền");
            logger.info("Đã cập nhật trạng thái thanh toán thành 'Đã hoàn tiền' cho đơn hàng {}", orderId);
        }

        sendCancelNotificationToAdmin(savedOrder, reason);

        Long userId = order.getUser().getUserId();
        String message = "Đơn hàng #" + orderId + " của bạn đã được hủy với lý do: " + reason;
        webSocketService.sendToUser(userId, "/queue/notifications", message);

        return savedOrder;
    }

    private void sendCancelNotificationToAdmin(Orders order, String reason) {
        try {
            String adminEmail = "baolgpc08011@fpt.edu.vn"; // Email admin
            String subject = "Thông báo: Đơn hàng #" + order.getOrderId() + " đã bị hủy";

            // Nội dung email HTML với background cho h2
            String htmlContent = "<!DOCTYPE html>" +
                    "<html lang='vi'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                    ".container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #f9f9f9; }" +
                    "h2 { color: white; text-align: center; background-color: rgb(251, 179, 33); padding: 10px; border-radius: 5px; }" +
                    "table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
                    "th, td { padding: 10px; border: 1px solid #ddd; }" +
                    "th { background-color: #f5f5f5; text-align: left; width: 30%; }" +
                    "td { background-color: #fff; }" +
                    ".highlight { color: #e74c3c; font-weight: bold; }" +
                    ".footer { text-align: center; font-size: 0.9em; color: #777; margin-top: 20px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<h2>Thông báo hủy đơn hàng</h2>" +
                    "<p>Kính gửi Shop,</p>" +
                    "<p>Một đơn hàng vừa bị hủy với thông tin chi tiết như sau:</p>" +
                    "<table>" +
                    "<tr><th>Mã đơn hàng</th><td>" + order.getOrderId() + "</td></tr>" +
                    "<tr><th>Khách hàng</th><td>" + order.getUser().getFullName() + "</td></tr>" +
                    "<tr><th>Số điện thoại</th><td>" + order.getUser().getPhone() + "</td></tr>" +
                    "<tr><th>Ngày đặt hàng</th><td>" + order.getOrderDate() + "</td></tr>" +
                    "<tr><th>Tổng tiền</th><td>" + order.getTotalAmount() + " VNĐ</td></tr>" +
                    "<tr><th>Lý do hủy</th><td class='highlight'>" + reason + "</td></tr>" +
                    "</table>" +
                    "<p>Vui lòng kiểm tra hệ thống để biết thêm chi tiết.</p>" +
                    "<div class='footer'>" +
                    "<p>Trân trọng,<br>Hệ thống PetCare</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            // Sử dụng EmailService để gửi email
            boolean success = emailService.sendHtmlEmail(adminEmail, subject, htmlContent);
            
            if (success) {
                logger.info("Email thông báo hủy đơn hàng #{} đã được gửi thành công đến Admin.", order.getOrderId());
            } else {
                logger.warn("Không thể gửi email thông báo hủy đơn hàng #{} đến Admin.", order.getOrderId());
            }
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email thông báo hủy đơn hàng #{} đến Admin: {}", order.getOrderId(), e.getMessage());
            e.printStackTrace(); // In stack trace để debug
        }
    }

    // đơn hàng online
    // Lấy các đơn hàng có type "ORDER ONLINE" và statusId = 4 (Hoàn thành)
    public List<OrderDTO> getCompletedOnlineOrders() {
        List<Orders> completedOnlineOrders = orderRepository.findByTypeAndStatusOrderStatusId("ORDER ONLINE", 4L);
        return completedOnlineOrders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    // Lấy các đơn hàng online trong khoảng thời gian từ ngày bắt đầu đến ngày kết thúc
    public List<OrderDTO> getOnlineOrdersByDateRange(Date startDate, Date endDate) {
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

        // Chỉ lấy các đơn hàng có type = "ORDER ONLINE" và statusId = 4
        List<Orders> onlineOrders = orderRepository.findByTypeAndStatusOrderStatusIdAndOrderDateBetween(
                "ORDER ONLINE", 4L, startDateInclusive, endDateInclusive);
        return onlineOrders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }
    //


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



    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Orders> userOrders = orderRepository.findByUserUserId(userId);
        return userOrders.stream().map(this::convertToOrderDTO).collect(Collectors.toList());
    }

    @Transactional
    public Orders updateOrderStatus(Long orderId, Long statusId, String reason) {
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

        // Lưu thông báo vào database trước
        Notification notification = notificationService.saveNotification(userId, message);

        // Gửi thông báo qua WebSocket với ID thực tế
        String webSocketMessage = "{\"id\": " + notification.getId() + ", \"message\": \"" + message + "\", \"orderId\": " + orderId + "}";
        try {
            webSocketService.sendToTopic("/topic/status", webSocketMessage); // Gửi broadcast với JSON
            System.out.println("✅ WebSocket notification broadcast to /topic/status: " + webSocketMessage);
        } catch (Exception e) {
            System.err.println("❌ Failed to send WebSocket notification to /topic/status: " + e.getMessage());
            logger.error("Failed to send WebSocket notification for orderId: " + orderId, e); // Ghi log chi tiết
        }
        // 12️⃣ Nếu trạng thái mới là "Đã hủy" (statusId = 5), gửi email thông báo cho người dùng
        if (statusId.equals(5L)) {
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Không có lý do cụ thể";
            }
            sendCancelNotificationToUser(savedOrder, reason);
        }

        return savedOrder;
    }

    private void sendCancelNotificationToUser(Orders order, String reason) {
        try {
            String userEmail = order.getUser().getEmail(); // Lấy email từ user
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("Không tìm thấy email của người dùng cho đơn hàng #{}", order.getOrderId());
                return;
            }
            
            String subject = "Thông báo: Đơn hàng #" + order.getOrderId() + " của bạn đã bị hủy";
            
            // Định dạng ngày giờ theo giờ Việt Nam
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            String formattedOrderDate = formatter.format(order.getOrderDate().toInstant());

            // Tạo nội dung HTML cho danh sách sản phẩm
            StringBuilder productTable = new StringBuilder();
            productTable.append("<h3>Danh sách sản phẩm:</h3>")
                    .append("<table>")
                    .append("<tr><th>Hình ảnh</th><th>Tên sản phẩm</th><th>Màu sắc</th><th>Kích cỡ</th><th>Số lượng</th><th>Giá</th></tr>");

            for (OrderDetails item : order.getOrderDetails()) {
                String imageUrl = item.getProductDetails().getProducts().getImage() != null
                        ? item.getProductDetails().getProducts().getImage()
                        : "https://via.placeholder.com/50"; // URL mặc định nếu không có hình ảnh
                productTable.append("<tr>")
                        .append("<td><img src='").append(imageUrl).append("' alt='Product Image' style='width: 50px; height: 50px; object-fit: cover;'/></td>")
                        .append("<td>").append(item.getProductDetails().getProducts().getProductName()).append("</td>")
                        .append("<td>").append(item.getProductDetails().getProductColors().getColorValue()).append("</td>")
                        .append("<td>").append(item.getProductDetails().getProductSizes().getSizeValue()).append("</td>")
                        .append("<td>").append(item.getQuantity()).append("</td>")
                        .append("<td>").append(item.getPrice()).append(" đ</td>")
                        .append("</tr>");
            }
            productTable.append("</table>");

            // Nội dung email HTML
            String htmlContent = "<!DOCTYPE html>" +
                    "<html lang='vi'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                    ".container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #f9f9f9; }" +
                    "h2 { color: white; text-align: center; background-color: #e74c3c; padding: 10px; border-radius: 5px; }" +
                    "h3 { color: #333; margin-top: 20px; }" +
                    "table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
                    "th, td { padding: 10px; border: 1px solid #ddd; vertical-align: middle; }" +
                    "th { background-color: #f5f5f5; text-align: left; }" +
                    "td { background-color: #fff; }" +
                    ".highlight { color: #e74c3c; font-weight: bold; }" +
                    ".footer { text-align: center; font-size: 0.9em; color: #777; margin-top: 20px; }" +
                    "img { display: block; margin: 0 auto; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<h2>Thông báo hủy đơn hàng</h2>" +
                    "<p>Kính gửi " + order.getUser().getFullName() + ",</p>" +
                    "<p>Chúng tôi rất tiếc phải thông báo rằng đơn hàng của bạn đã bị hủy với thông tin chi tiết như sau:</p>" +
                    "<table>" +
                    "<tr><th>Ngày đặt hàng</th><td>" + formattedOrderDate + "</td></tr>" +
                    "<tr><th>Tổng tiền</th><td>" + order.getTotalAmount() + " đ</td></tr>" +
                    "<tr><th>Lý do hủy</th><td class='highlight'>" + reason + "</td></tr>" +
                    "</table>" +
                    productTable.toString() + // Chèn bảng sản phẩm
                    "<p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua email hoặc số điện thoại hỗ trợ.</p>" +
                    "<div class='footer'>" +
                    "<p>Trân trọng,<br>Hệ thống PetCare</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            // Sử dụng EmailService để gửi email
            boolean success = emailService.sendHtmlEmail(userEmail, subject, htmlContent);
            
            if (success) {
                logger.info("Email thông báo hủy đơn hàng #{} đã được gửi thành công đến người dùng: {}", 
                           order.getOrderId(), userEmail);
            } else {
                logger.warn("Không thể gửi email thông báo hủy đơn hàng #{} đến người dùng: {}", 
                          order.getOrderId(), userEmail);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email thông báo hủy đơn hàng #{} đến người dùng: {}", 
                       order.getOrderId(), e.getMessage());
            e.printStackTrace(); // In stack trace để debug
        }
    }

    public List<OrderDTO> getOrdersByVoucherId(Long voucherId) {
        List<Orders> orders = orderRepository.findOrdersByVoucherId(voucherId);
        return orders.stream().map(this::convertToOrderDTO).collect(Collectors.toList());
    }

    public boolean checkOrderExists(Long orderId) {
        return orderRepository.existsById(orderId);
    }

    public boolean checkOrderExists(String orderIdStr) {
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            return false;
        }
        try {
            Long orderId = Long.parseLong(orderIdStr);
            return checkOrderExists(orderId);
        } catch (NumberFormatException e) {
            logger.warn("Invalid orderId format: {}", orderIdStr);
            return false;
        }
    }

    public Orders getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    public Orders updateOrderStatusAndPayment(Long orderId, Long statusId, String paymentStatus) {
        Orders order = getOrderById(orderId);
        
        logger.info("Updating order status and payment: orderId={}, statusId={}, paymentStatus={}", 
                orderId, statusId, paymentStatus);
        
        String oldStatus = order.getStatusOrder() != null ? order.getStatusOrder().getStatusName() : "null";
        String oldPaymentStatus = order.getPaymentStatus();
        
        if (statusId != null) {
            // Kiểm tra trạng thái hiện tại
            Long currentStatusId = order.getStatusOrder().getStatusId();
            
            // Chặn cập nhật từ các trạng thái cuối
            List<Long> finalStatuses = Arrays.asList(4L, 5L, 6L);
            if (finalStatuses.contains(currentStatusId)) {
                throw new RuntimeException("Không thể cập nhật trạng thái từ 'Hoàn thành', 'Đã hủy' hoặc 'Trả hàng'.");
            }
            
            // Kiểm tra tính hợp lệ của việc chuyển trạng thái
            if (currentStatusId == 1L && statusId != 2L && statusId != 5L) {
                throw new RuntimeException("Đơn hàng ở trạng thái 'Chờ xác nhận' chỉ có thể chuyển sang 'Đang vận chuyển' hoặc 'Đã hủy'.");
            }
            
            if (currentStatusId == 2L && statusId != 3L && statusId != 5L) {
                throw new RuntimeException("Đơn hàng ở trạng thái 'Đang vận chuyển' chỉ có thể chuyển sang 'Chờ giao hàng' hoặc 'Đã hủy'.");
            }
            
            if (currentStatusId == 3L && statusId != 4L && statusId != 5L) {
                throw new RuntimeException("Đơn hàng ở trạng thái 'Chờ giao hàng' chỉ có thể chuyển sang 'Hoàn thành' hoặc 'Đã hủy'.");
            }
            
            StatusOrder statusOrder = statusOrderRepository.findById(statusId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy trạng thái với ID: " + statusId));
            order.setStatusOrder(statusOrder);
            
            // Nếu chuyển sang trạng thái "Hoàn thành" (statusId = 4) và không có paymentStatus cụ thể
            if (statusId == 4L && paymentStatus == null) {
                logger.info("Auto setting paymentStatus to 'Đã thanh toán' for completed order: {}", orderId);
                order.setPaymentStatus("Đã thanh toán");
            }
        }
        
        // Chỉ cập nhật paymentStatus khi có giá trị rõ ràng
        if (paymentStatus != null) {
            logger.info("Explicitly updating paymentStatus for orderId={} from '{}' to '{}'", 
                    orderId, oldPaymentStatus, paymentStatus);
            order.setPaymentStatus(paymentStatus);
        }
        
        Orders savedOrder = orderRepository.save(order);
        
        logger.info("Updated order: {} status from '{}' to '{}', paymentStatus from '{}' to '{}'", 
                orderId, oldStatus, savedOrder.getStatusOrder().getStatusName(), 
                oldPaymentStatus, savedOrder.getPaymentStatus());
        
        // Nếu trạng thái mới là "Đã hủy" (statusId = 5), khôi phục tồn kho
        if (statusId != null && statusId == 5L) {
            try {
                for (OrderDetails orderDetail : order.getOrderDetails()) {
                    int updated = productDetailsRepository.updateStockcancel(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()
                    );
                    if (updated > 0) {
                        logger.info("Đã khôi phục {} sản phẩm {} vào kho khi cập nhật trạng thái sang Đã hủy", 
                                orderDetail.getQuantity(), 
                                orderDetail.getProductDetails().getProductDetailId());
                    } else {
                        logger.error("Không thể khôi phục tồn kho cho sản phẩm {} khi hủy đơn", 
                                orderDetail.getProductDetails().getProductDetailId());
                    }
                }
                logger.info("Đã khôi phục tồn kho cho đơn hàng {} khi cập nhật trạng thái sang Đã hủy", orderId);
            } catch (Exception e) {
                logger.error("Lỗi khi khôi phục tồn kho cho đơn hàng {} sau khi cập nhật trạng thái: {}", 
                        orderId, e.getMessage());
            }
        }
        
        return savedOrder;
    }

    // Kiểm tra đơn hàng MoMo có tồn tại theo momoOrderId
    public boolean checkMomoOrderExists(String momoOrderId) {
        // Sử dụng cách lưu momoOrderId trong CheckoutRequestDTO
        if (momoOrderId == null || momoOrderId.isEmpty()) {
            logger.warn("checkMomoOrderExists called with null or empty momoOrderId");
            return false;
        }
        
        // Kiểm tra trong bảng Orders nếu có cột momoOrderId
        logger.info("Checking if MoMo order exists with momoOrderId: {}", momoOrderId);
        List<Orders> orders = orderRepository.findByMomoOrderId(momoOrderId);
        boolean exists = !orders.isEmpty();
        
        if (exists) {
            logger.info("Found {} existing orders with momoOrderId: {}", orders.size(), momoOrderId);
            for (Orders order : orders) {
                logger.info("Existing order details: orderId={}, status={}, paymentStatus={}", 
                            order.getOrderId(), 
                            order.getStatusOrder() != null ? order.getStatusOrder().getStatusName() : "null",
                            order.getPaymentStatus());
            }
        } else {
            logger.info("No existing orders found with momoOrderId: {}", momoOrderId);
        }
        
        return exists;
    }

    // Thêm lý do hủy đơn hàng
    @Transactional
    public void addCancellationReason(Long orderId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            logger.warn("Empty cancellation reason provided for orderId: {}", orderId);
            return;
        }
        
        Orders order = getOrderById(orderId);
        if (order == null) {
            logger.error("Cannot add cancellation reason: Order not found with ID: {}", orderId);
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
        }
        
        try {
            // Lưu lý do hủy đơn hàng (có thể lưu vào một trường mới hoặc bảng phụ)
            // Ví dụ: order.setCancellationReason(reason);
            logger.info("Cancellation reason set for order {}: {}", orderId, reason);
            orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Error saving cancellation reason for orderId {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể lưu lý do hủy đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin thanh toán MoMo cho đơn hàng
     * @param orderId ID đơn hàng
     * @param momoOrderId ID đơn hàng từ MoMo
     * @param momoTransId ID giao dịch từ MoMo
     * @param momoAmount Số tiền thanh toán qua MoMo
     * @return Đối tượng đơn hàng đã cập nhật
     */
    @Transactional
    public Orders updateMomoInfo(Long orderId, String momoOrderId, String momoTransId, String momoAmount) {
        // Tìm đơn hàng
        Orders order = getOrderById(orderId);
        
        // Kiểm tra phương thức thanh toán
        if (!"MoMo".equals(order.getPaymentMethod())) {
            logger.warn("Attempt to update MoMo info for non-MoMo payment method: {}", order.getPaymentMethod());
            throw new RuntimeException("Không thể cập nhật thông tin MoMo cho đơn hàng không thanh toán qua MoMo");
        }
        
        // Cập nhật thông tin MoMo
        if (momoOrderId != null && !momoOrderId.isEmpty()) {
            order.setMomoOrderId(momoOrderId);
        }
        
        if (momoTransId != null && !momoTransId.isEmpty()) {
            order.setMomoTransId(momoTransId);
        }
        
        // Xử lý trường hợp momoAmount là undefined, NaN hoặc không hợp lệ
        if (momoAmount != null && !momoAmount.isEmpty() && 
            !"undefined".equals(momoAmount) && !"NaN".equals(momoAmount)) {
            // Thử parse giá trị để xác nhận là số hợp lệ
            try {
                // Kiểm tra xem có phải là số hợp lệ không
                double amount = Double.parseDouble(momoAmount);
                if (Double.isNaN(amount)) {
                    throw new NumberFormatException("Value is NaN");
                }
                logger.info("Cập nhật momoAmount cho đơn hàng {}: {}", orderId, momoAmount);
                order.setMomoAmount(momoAmount);
            } catch (NumberFormatException e) {
                // Nếu không phải số hợp lệ, sử dụng giá trị totalAmount thay thế
                String totalAmountStr = String.valueOf(Math.round(order.getTotalAmount()));
                order.setMomoAmount(totalAmountStr);
                logger.info("momoAmount không phải là số hợp lệ ({}), thay thế bằng totalAmount cho đơn hàng {}: {}", 
                        momoAmount, orderId, totalAmountStr);
            }
        } else {
            // Sử dụng totalAmount nếu momoAmount không hợp lệ
            String totalAmountStr = String.valueOf(Math.round(order.getTotalAmount()));
            order.setMomoAmount(totalAmountStr);
            logger.info("Thay thế momoAmount không hợp lệ ({}) bằng totalAmount cho đơn hàng {}: {}", 
                    momoAmount, orderId, totalAmountStr);
        }
        
        logger.info("Updated MoMo info for orderId={}: momoOrderId={}, momoTransId={}, momoAmount={}", 
                orderId, momoOrderId, momoTransId, order.getMomoAmount());
        
        // Lưu và trả về đơn hàng cập nhật
        return orderRepository.save(order);
    }

    /**
     * Tìm các đơn hàng theo phương thức thanh toán
     * @param paymentMethod Phương thức thanh toán cần tìm
     * @return Danh sách đơn hàng 
     */
    public List<Orders> findByPaymentMethod(String paymentMethod) {
        return orderRepository.findByPaymentMethod(paymentMethod);
    }
    
    /**
     * Lưu đối tượng Orders vào cơ sở dữ liệu
     * @param order Đối tượng Orders cần lưu
     * @return Đối tượng Orders đã được lưu
     */
    public Orders save(Orders order) {
        return orderRepository.save(order);
    }

    /**
     * Lấy thông tin đơn hàng dưới dạng DTO theo ID
     * @param orderId ID của đơn hàng cần tìm
     * @return OrderDTO chứa thông tin đơn hàng
     */
    public OrderDTO getOrderDTOById(Long orderId) {
        Orders order = getOrderById(orderId);
        return convertToOrderDTO(order);
    }
}
