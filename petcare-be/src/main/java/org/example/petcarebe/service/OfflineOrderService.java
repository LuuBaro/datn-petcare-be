package org.example.petcarebe.service;

import org.example.petcarebe.dto.OfflineOrderDTO;
import org.example.petcarebe.dto.PointInfoDTO;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfflineOrderService {

    @Autowired
    private CartDetailsRepository cartDetailsRepository;

    @Autowired
    private OrderRepository ordersRepository;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Transactional
    public OfflineOrderDTO.OfflineOrderResponse createOfflineOrder(OfflineOrderDTO.OfflineOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getUserId()));

        Orders order = new Orders();
        order.setOrderDate(new Date());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("Đã thanh toán");
        order.setType("OFFLINE");
        order.setUser(user);
        order.setShippingCost(0);
        order.setPointUsed(0); // Điểm sử dụng sẽ được cập nhật trong applyDiscount nếu có

        List<OrderDetails> orderDetails = request.getItems().stream().map(item -> {
            ProductDetails productDetail = productDetailsRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + item.getProductDetailId()));

            if (productDetail.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Không đủ hàng tồn kho cho sản phẩm: " + productDetail.getProducts().getProductName());
            }

            OrderDetails detail = new OrderDetails();
            detail.setProductDetails(productDetail);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(productDetail.getPrice());
            detail.setOrders(order);

            productDetail.setQuantity(productDetail.getQuantity() - item.getQuantity());
            productDetailsRepository.save(productDetail);

            return detail;
        }).collect(Collectors.toList());

        float totalAmount = (float) orderDetails.stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getPrice())
                .sum();
        order.setTotalAmount(totalAmount);
        order.setOrderDetails(orderDetails);

        int pointsEarned = 0;
        int totalPoints = 0;
        Point customerPoint = null;
        if (request.isAccumulatePoints()) {
            if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty()) {
                throw new RuntimeException("Vui lòng cung cấp số điện thoại để tích điểm.");
            }

            customerPoint = pointRepository.findByPhone(request.getCustomerPhone())
                    .orElseGet(() -> {
                        Point newPoint = new Point();
                        newPoint.setPhone(request.getCustomerPhone());
                        newPoint.setName(request.getCustomerName() != null ? request.getCustomerName() : "Khách vãng lai");
                        newPoint.setTotalPoint(0);
                        newPoint.setUser(user);
                        return newPoint;
                    });

            int previousTotalSpent = customerPoint.getTotalPoint() * 100000;
            int newTotalSpent = previousTotalSpent + (int) totalAmount;
            totalPoints = newTotalSpent / 100000;
            pointsEarned = totalPoints - customerPoint.getTotalPoint();

            customerPoint.setTotalPoint(totalPoints);
            customerPoint = pointRepository.save(customerPoint);

            order.setPoint(customerPoint);
            order.setPointEarned(pointsEarned);
        }

        Orders savedOrder = ordersRepository.save(order);

        OfflineOrderDTO.OfflineOrderResponse response = new OfflineOrderDTO.OfflineOrderResponse();
        response.setOrderId(savedOrder.getOrderId());
        response.setPaymentMethod(savedOrder.getPaymentMethod());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setStatus(savedOrder.getPaymentStatus());
        response.setPointsEarned(pointsEarned);
        response.setUserId(user.getUserId());
        response.setStaffName(user.getFullName());
        response.setTotalPoints(totalPoints);

        return response;
    }

    @Transactional
    public OfflineOrderDTO.OfflineOrderResponse applyDiscount(OfflineOrderDTO.OfflineOrderRequest request) {
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty()) {
            throw new RuntimeException("Vui lòng cung cấp số điện thoại để áp dụng giảm giá.");
        }

        Point customerPoint = pointRepository.findByPhone(request.getCustomerPhone())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin điểm với số điện thoại: " + request.getCustomerPhone()));

        int pointsToUse = request.getPointsToUse(); // Lấy số điểm muốn sử dụng từ request
        if (pointsToUse < 10 || pointsToUse % 10 != 0) {
            throw new RuntimeException("Số điểm sử dụng phải là bội số của 10 và tối thiểu 10 điểm.");
        }
        if (customerPoint.getTotalPoint() < pointsToUse) {
            throw new RuntimeException("Không đủ điểm để áp dụng giảm giá (cần tối thiểu " + pointsToUse + " điểm).");
        }

        // Tạo đơn hàng bằng hàm createOfflineOrder
        OfflineOrderDTO.OfflineOrderResponse response = createOfflineOrder(request);

        // Lấy đơn hàng vừa tạo để áp dụng giảm giá
        Orders order = ordersRepository.findById(response.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng vừa tạo."));

        float originalTotalAmount = order.getTotalAmount();
        float discountAmount = (pointsToUse / 10) * 30000; // Mỗi 10 điểm giảm 30,000 VNĐ
        float newTotalAmount = originalTotalAmount - discountAmount;

        if (newTotalAmount < 0) {
            newTotalAmount = 0; // Không âm
        }

        order.setTotalAmount(newTotalAmount);
        order.setPointUsed(pointsToUse); // Ghi nhận số điểm đã sử dụng
        ordersRepository.save(order);

        // Cập nhật điểm của khách hàng
        customerPoint.setTotalPoint(customerPoint.getTotalPoint() - pointsToUse);
        pointRepository.save(customerPoint);

        // Cập nhật response
        response.setTotalAmount(newTotalAmount);
        response.setTotalPoints(customerPoint.getTotalPoint());

        return response;
    }

    public PointInfoDTO getPointsByPhone(String phone) {
        Point point = pointRepository.findByPhone(phone)
                .orElse(null);
        if (point == null) {
            return new PointInfoDTO("Khách vãng lai", 0);
        }
        return new PointInfoDTO(point.getName(), point.getTotalPoint());
    }

    //  hàm getAllOrders
    public List<OfflineOrderDTO.OfflineOrderResponse> getAllOrders() {
        List<Orders> orders = ordersRepository.findAllByType("OFFLINE");
        return orders.stream().map(order -> {
            OfflineOrderDTO.OfflineOrderResponse response = new OfflineOrderDTO.OfflineOrderResponse();
            response.setOrderId(order.getOrderId());
            response.setPaymentMethod(order.getPaymentMethod());
            response.setTotalAmount(order.getTotalAmount());
            response.setStatus(order.getPaymentStatus());
            response.setPointsEarned(order.getPointEarned());
            response.setUserId(order.getUser() != null ? order.getUser().getUserId() : null);
            response.setStaffName(order.getUser() != null ? order.getUser().getFullName() : "Không xác định");
            response.setOrderDate(order.getOrderDate());

            Point point = order.getPoint();
            if (point != null) {
                response.setCustomerName(point.getName());
                response.setCustomerPhone(point.getPhone());
                response.setTotalPoints(point.getTotalPoint());
            } else {
                response.setCustomerName(null);
                response.setCustomerPhone(null);
                response.setTotalPoints(0);
            }

            List<OrderDetails> orderDetails = order.getOrderDetails();
            List<OfflineOrderDTO.OrderItemResponse> items;
            if (orderDetails == null || orderDetails.isEmpty()) {
                items = Collections.emptyList();
                System.out.println("Cảnh báo: Không có chi tiết đơn hàng cho Order ID: " + order.getOrderId());
            } else {
                items = orderDetails.stream().map(detail -> {
                    OfflineOrderDTO.OrderItemResponse item = new OfflineOrderDTO.OrderItemResponse();
                    ProductDetails productDetails = detail.getProductDetails();
                    if (productDetails == null) {
                        System.err.println("Lỗi: ProductDetails là null cho OrderDetails ID: " + detail.getOrderDetailsId());
                        item.setProductName("Sản phẩm không xác định");
                        item.setProductDetailId(null);
                        item.setColorValue("Không xác định");
                        item.setSizeValue("Không xác định");
                        item.setWeightValue(0.0f); // Giá trị mặc định cho float
                    } else {
                        item.setProductDetailId(productDetails.getProductDetailId());
                        Products product = productDetails.getProducts();
                        item.setProductName(product != null ? product.getProductName() : "Sản phẩm không xác định");

                        // Lấy màu sắc, kích thước, cân nặng
                        ProductColors color = productDetails.getProductColors();
                        item.setColorValue(color != null ? color.getColorValue() : "Không xác định");

                        ProductSizes size = productDetails.getProductSizes();
                        item.setSizeValue(size != null ? size.getSizeValue() : "Không xác định");

                        Weights weight = productDetails.getWeights();
                        item.setWeightValue(weight != null ? weight.getWeightValue() : 0.0f); // float
                    }
                    item.setPrice(detail.getPrice());
                    item.setQuantity(detail.getQuantity());
                    return item;
                }).collect(Collectors.toList());
            }
            response.setItems(items);
            return response;
        }).collect(Collectors.toList());
    }

    public List<OfflineOrderDTO.OfflineOrderResponse> getOrdersByDateRange(Date startDate, Date endDate) {
        List<Orders> orders = ordersRepository.findOfflineOrdersByDateRange(startDate, endDate);
        return orders.stream().map(order -> {
            OfflineOrderDTO.OfflineOrderResponse response = new OfflineOrderDTO.OfflineOrderResponse();
            response.setOrderId(order.getOrderId());
            response.setPaymentMethod(order.getPaymentMethod());
            response.setTotalAmount(order.getTotalAmount());
            response.setStatus(order.getPaymentStatus());
            response.setPointsEarned(order.getPointEarned());
            response.setUserId(order.getUser() != null ? order.getUser().getUserId() : null);
            response.setStaffName(order.getUser() != null ? order.getUser().getFullName() : "Không xác định");
            response.setOrderDate(order.getOrderDate());

            Point point = order.getPoint();
            if (point != null) {
                response.setCustomerName(point.getName());
                response.setCustomerPhone(point.getPhone());
                response.setTotalPoints(point.getTotalPoint());
            } else {
                response.setCustomerName(null);
                response.setCustomerPhone(null);
                response.setTotalPoints(0);
            }

            List<OrderDetails> orderDetails = order.getOrderDetails();
            List<OfflineOrderDTO.OrderItemResponse> items;
            if (orderDetails == null || orderDetails.isEmpty()) {
                items = Collections.emptyList();
                System.out.println("Cảnh báo: Không có chi tiết đơn hàng cho Order ID: " + order.getOrderId());
            } else {
                items = orderDetails.stream().map(detail -> {
                    OfflineOrderDTO.OrderItemResponse item = new OfflineOrderDTO.OrderItemResponse();
                    ProductDetails productDetails = detail.getProductDetails();
                    if (productDetails == null) {
                        System.err.println("Lỗi: ProductDetails là null cho OrderDetails ID: " + detail.getOrderDetailsId());
                        item.setProductName("Sản phẩm không xác định");
                        item.setProductDetailId(null);
                        item.setColorValue("Không xác định");
                        item.setSizeValue("Không xác định");
                        item.setWeightValue(0.0f);
                    } else {
                        item.setProductDetailId(productDetails.getProductDetailId());
                        Products product = productDetails.getProducts();
                        item.setProductName(product != null ? product.getProductName() : "Sản phẩm không xác định");
                        ProductColors color = productDetails.getProductColors();
                        item.setColorValue(color != null ? color.getColorValue() : "Không xác định");
                        ProductSizes size = productDetails.getProductSizes();
                        item.setSizeValue(size != null ? size.getSizeValue() : "Không xác định");
                        Weights weight = productDetails.getWeights();
                        item.setWeightValue(weight != null ? weight.getWeightValue() : 0.0f);
                    }
                    item.setPrice(detail.getPrice());
                    item.setQuantity(detail.getQuantity());
                    return item;
                }).collect(Collectors.toList());
            }
            response.setItems(items);
            return response;
        }).collect(Collectors.toList());
    }


    // giỏ hàng offline
    @Transactional
    public CartDetails addProductToOfflineCart(Long userId, Long productDetailId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));

        ProductDetails productDetail = productDetailsRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productDetailId));

        if (productDetail.getQuantity() < quantity) {
            throw new RuntimeException("Không đủ hàng tồn kho cho sản phẩm: " + productDetail.getProducts().getProductName());
        }

        CartDetails existingCartDetail = cartDetailsRepository.findByUserAndProductDetails(user, productDetail);

        if (existingCartDetail != null) {
            existingCartDetail.setQuantityItem(existingCartDetail.getQuantityItem() + quantity);
            return cartDetailsRepository.save(existingCartDetail);
        } else {
            CartDetails cartDetail = new CartDetails();
            cartDetail.setUser(user);
            cartDetail.setProductDetails(productDetail);
            cartDetail.setQuantityItem(quantity);
            return cartDetailsRepository.save(cartDetail);
        }
    }

    @Transactional
    public void removeProductFromOfflineCart(Long userId, Long productDetailId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));

        ProductDetails productDetail = productDetailsRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productDetailId));

        CartDetails cartDetail = cartDetailsRepository.findByUserAndProductDetails(user, productDetail);

        if (cartDetail == null) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng.");
        }

        cartDetailsRepository.delete(cartDetail);
    }

    public List<CartDetails> getOfflineCartDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));
        return cartDetailsRepository.findByUser(user);
    }
}