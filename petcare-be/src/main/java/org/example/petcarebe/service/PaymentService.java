package org.example.petcarebe.service;

import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.CartDetailsRepository;
import org.example.petcarebe.repository.OrderDetailsRepository;
import org.example.petcarebe.repository.OrderRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PaymentService {
    private final OrderRepository ordersRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final UserRepository userRepository;
    private final CartDetailsRepository cartDetailsRepository;

    public PaymentService(OrderRepository ordersRepository, OrderDetailsRepository orderDetailsRepository,
                          UserRepository userRepository, CartDetailsRepository cartDetailsRepository) {
        this.ordersRepository = ordersRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.userRepository = userRepository;
        this.cartDetailsRepository = cartDetailsRepository;
    }

    @Transactional
    public ResponseEntity<?> processPayment(Long userId) {
        // 1️⃣ Kiểm tra User có tồn tại không
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User không tồn tại");
        }

        // 2️⃣ Lấy tất cả sản phẩm trong giỏ hàng của User
        List<CartDetails> cartItems = cartDetailsRepository.findByUser_UserId(userId);
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("Giỏ hàng trống");
        }

        // 3️⃣ Tạo đơn hàng mới
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setPaymentStatus(0); // 0: Chưa thanh toán, 1: Đã thanh toán
        order.setShippingCost(50000); // Phí vận chuyển cố định
        order.setTotalAmount(calculateTotal(cartItems) + order.getShippingCost());
        order.setPaymentMethod("COD"); // Mặc định là Thanh toán khi nhận hàng
        ordersRepository.save(order);

        // 4️⃣ Lưu chi tiết đơn hàng từ giỏ hàng
        for (CartDetails item : cartItems) {
            OrderDetails orderDetails = new OrderDetails();
            orderDetails.setOrders(order);
            orderDetails.setQuantity(item.getQuantityItem());
            orderDetails.setPrice(item.getProductDetails().getPrice());
            orderDetails.setProductDetail(item.getProductDetails());

            orderDetailsRepository.save(orderDetails);
        }

        // 5️⃣ Xóa tất cả sản phẩm trong giỏ hàng của User
        cartDetailsRepository.deleteAllByUser_UserId(userId);

        return ResponseEntity.ok("Thanh toán thành công, mã đơn hàng: " + order.getOrderId());
    }

    private float calculateTotal(List<CartDetails> cartItems) {
        float total = 0;
        for (CartDetails item : cartItems) {
            total += item.getProductDetails().getPrice() * item.getQuantityItem();
        }
        return total;
    }
}
