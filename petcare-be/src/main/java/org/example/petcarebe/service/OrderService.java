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
        order.setPointEarned(0);
        order.setPointUsed(0);

        // 3️⃣ Kiểm tra voucher
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId()).orElse(null);
            order.setVoucher(voucher);
        }

        // 4️⃣ Thêm chi tiết đơn hàng
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO item : request.getItems()) {
            // Kiểm tra sản phẩm
            ProductDetails product = productDetailsRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductDetailId()));

            // Kiểm tra tồn kho
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + item.getProductDetailId());
            }

            // Giảm số lượng sản phẩm
//            product.setQuantity(product.getQuantity() - item.getQuantity());
//            productDetailsRepository.save(product);

            // Tạo OrderDetails
            OrderDetails orderDetail = new OrderDetails();
            orderDetail.setOrders(order);
            orderDetail.setProductDetails(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(item.getPrice());

            orderDetailsList.add(orderDetail);

            // Tính tổng tiền
            totalAmount = totalAmount.add(BigDecimal.valueOf(item.getQuantity()).multiply(BigDecimal.valueOf(item.getPrice())));
        }

        // 5️⃣ Cập nhật tổng tiền
        order.setTotalAmount(totalAmount.add(BigDecimal.valueOf(order.getShippingCost())).floatValue());

        // 6️⃣ Thêm chi tiết vào đơn hàng
        order.setOrderDetails(orderDetailsList);

        // 7️⃣ Lưu đơn hàng (tự động lưu OrderDetails do cascade = CascadeType.ALL)
        orderRepository.save(order);

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

}
