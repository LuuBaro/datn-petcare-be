package org.example.petcarebe.service;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.OrderDetailDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.dto.request.OrderItemDTO;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
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

    public void clearCartDetailsByUserId(Long cartDetailId) {
        cartDetailsService.deleteCartDetails(cartDetailId);
    }


    @Transactional
    public Orders checkout(CheckoutRequestDTO request) {
        // 1️⃣ Kiểm tra người dùng
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️⃣ Tạo đơn hàng
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCost(request.getShippingCost());
        order.setPaymentStatus("PENDING");
        order.setStatusOrder(statusOrderRepository.findById(1L).orElse(null));
        order.setType(request.getType());
        order.setPointEarned(0);
        order.setPointUsed(0);

        // 3️⃣ Kiểm tra voucher
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId()).orElse(null);
            order.setVoucher(voucher);
        }

        // 4️⃣ Thêm chi tiết đơn hàng và cập nhật số lượng tồn kho
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO item : request.getItems()) {
            ProductDetails product = productDetailsRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductDetailId()));

            // Kiểm tra tồn kho
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("đã hết hàng hoặc không đủ số lượng!");
            }

            // Tạo OrderDetails
            OrderDetails orderDetail = new OrderDetails();
            orderDetail.setOrders(order);
            orderDetail.setProductDetails(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(item.getPrice());

            orderDetailsList.add(orderDetail);
            totalAmount = totalAmount.add(
                    BigDecimal.valueOf(item.getQuantity()).multiply(BigDecimal.valueOf(item.getPrice()))
            );

            int updated = productDetailsRepository.updateStock(item.getProductDetailId(), item.getQuantity());
            if (updated == 0) {
                throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + item.getProductDetailId());
            }

        }

        // 5️⃣ Cập nhật tổng tiền
        order.setTotalAmount(totalAmount.add(BigDecimal.valueOf(order.getShippingCost())).floatValue());

        // 6️⃣ Thêm chi tiết vào đơn hàng
        order.setOrderDetails(orderDetailsList);

        // 7️⃣ Lưu đơn hàng
        orderRepository.save(order);

        // 8️⃣ Xóa giỏ hàng sau khi thanh toán thành công
        cartDetailsService.clearCartDetailsByUserId(request.getUserId());

        return order;
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



}
