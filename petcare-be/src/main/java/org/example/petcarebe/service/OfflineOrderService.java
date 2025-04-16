package org.example.petcarebe.service;

import org.example.petcarebe.dto.OfflineOrderDTO;
import org.example.petcarebe.dto.PointInfoDTO;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        order.setPointUsed(0);

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
        if (request.isAccumulatePoints() && request.getCustomerPhone() != null && !request.getCustomerPhone().isEmpty()) {
            customerPoint = pointRepository.findByPhone(request.getCustomerPhone())
                    .orElseGet(() -> {
                        Point newPoint = new Point();
                        newPoint.setPhone(request.getCustomerPhone());
                        newPoint.setName(request.getCustomerName() != null ? request.getCustomerName() : "Khách vãng lai");
                        newPoint.setTotalPoint(0);
                        newPoint.setUser(user);
                        return newPoint;
                    });

            // Logic: 10,000 VND = 1 điểm
            int previousTotalSpent = customerPoint.getTotalPoint() * 10000; // Mỗi điểm tương ứng 10,000 VND đã chi tiêu
            int newTotalSpent = previousTotalSpent + (int) totalAmount;
            totalPoints = newTotalSpent / 10000; // Tổng điểm dựa trên tổng chi tiêu
            pointsEarned = totalPoints - customerPoint.getTotalPoint();

            customerPoint.setTotalPoint(totalPoints);
            customerPoint = pointRepository.save(customerPoint);

            order.setPoint(customerPoint);
            order.setPointEarned(pointsEarned);
        }

        Orders savedOrder = ordersRepository.save(order);

        // Xóa giỏ hàng của tab sau khi thanh toán
        Integer tabId = request.getTabId();
        if (tabId != null) {
            cartDetailsRepository.deleteByUserIdAndTabId(request.getUserId(), tabId);
        }

        OfflineOrderDTO.OfflineOrderResponse response = new OfflineOrderDTO.OfflineOrderResponse();
        response.setOrderId(savedOrder.getOrderId());
        response.setPaymentMethod(savedOrder.getPaymentMethod());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setStatus(savedOrder.getPaymentStatus());
        response.setPointsEarned(pointsEarned);
        response.setUserId(user.getUserId());
        response.setStaffName(user.getFullName());
        response.setTotalPoints(totalPoints);
        response.setOrderDate(savedOrder.getOrderDate());

        return response;
    }

    @Transactional
    public OfflineOrderDTO.OfflineOrderResponse applyDiscount(OfflineOrderDTO.OfflineOrderRequest request) {
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty()) {
            throw new RuntimeException("Vui lòng cung cấp số điện thoại để áp dụng giảm giá.");
        }

        Point customerPoint = pointRepository.findByPhone(request.getCustomerPhone())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin điểm với số điện thoại: " + request.getCustomerPhone()));

        int pointsToUse = request.getPointsToUse();
        // Logic mới: Phải có ít nhất 100 điểm và chỉ sử dụng bội số của 100
        if (pointsToUse < 100 || pointsToUse % 100 != 0) {
            throw new RuntimeException("Số điểm sử dụng phải là bội số của 100 và tối thiểu 100 điểm.");
        }
        if (customerPoint.getTotalPoint() < pointsToUse) {
            throw new RuntimeException("Không đủ điểm để áp dụng giảm giá (cần tối thiểu " + pointsToUse + " điểm).");
        }

        OfflineOrderDTO.OfflineOrderResponse response = createOfflineOrder(request);

        Orders order = ordersRepository.findById(response.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng vừa tạo."));

        float originalTotalAmount = order.getTotalAmount();
        // Logic mới: 100 điểm = 30,000 VND giảm giá
        float discountAmount = (pointsToUse / 100) * 30000; // Mỗi 100 điểm giảm 30,000 VND
        float newTotalAmount = Math.max(0, originalTotalAmount - discountAmount);

        order.setTotalAmount(newTotalAmount);
        order.setPointUsed(pointsToUse);
        ordersRepository.save(order);

        customerPoint.setTotalPoint(customerPoint.getTotalPoint() - pointsToUse);
        pointRepository.save(customerPoint);

        response.setTotalAmount(newTotalAmount);
        response.setTotalPoints(customerPoint.getTotalPoint());

        return response;
    }

    // Các phương thức khác giữ nguyên
    public List<OfflineOrderDTO.OfflineOrderResponse> getAllOrders() {
        List<Orders> orders = ordersRepository.findAllByType("OFFLINE");
        return mapOrdersToResponse(orders);
    }

    public List<OfflineOrderDTO.OfflineOrderResponse> getOrdersByDateRange(Date startDate, Date endDate) {
        List<Orders> orders = ordersRepository.findOfflineOrdersByDateRange(startDate, endDate);
        return mapOrdersToResponse(orders);
    }

    private List<OfflineOrderDTO.OfflineOrderResponse> mapOrdersToResponse(List<Orders> orders) {
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
            }

            List<OrderDetails> orderDetails = order.getOrderDetails();
            List<OfflineOrderDTO.OrderItemResponse> items = (orderDetails == null || orderDetails.isEmpty())
                    ? Collections.emptyList()
                    : orderDetails.stream().map(detail -> {
                OfflineOrderDTO.OrderItemResponse item = new OfflineOrderDTO.OrderItemResponse();
                ProductDetails productDetails = detail.getProductDetails();
                if (productDetails != null) {
                    item.setProductDetailId(productDetails.getProductDetailId());
                    Products product = productDetails.getProducts();
                    item.setProductName(product != null ? product.getProductName() : "Sản phẩm không xác định");
                    item.setColorValue(productDetails.getProductColors() != null ? productDetails.getProductColors().getColorValue() : "Không xác định");
                    item.setSizeValue(productDetails.getProductSizes() != null ? productDetails.getProductSizes().getSizeValue() : "Không xác định");
                    item.setWeightValue(productDetails.getWeights() != null ? productDetails.getWeights().getWeightValue() : 0.0f);
                } else {
                    item.setProductName("Sản phẩm không xác định");
                    item.setColorValue("Không xác định");
                    item.setSizeValue("Không xác định");
                    item.setWeightValue(0.0f);
                }
                item.setPrice(detail.getPrice());
                item.setQuantity(detail.getQuantity());
                return item;
            }).collect(Collectors.toList());
            response.setItems(items);
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public CartDetails addProductToOfflineCart(Long userId, Long productDetailId, int quantity, Integer tabId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));
        ProductDetails productDetail = productDetailsRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productDetailId));

        if (productDetail.getQuantity() < quantity) {
            throw new RuntimeException("Không đủ hàng tồn kho cho sản phẩm: " + productDetail.getProducts().getProductName());
        }

        CartDetails existingCartDetail = cartDetailsRepository.findByUserAndProductDetailsAndTabId(user, productDetail, tabId);
        if (existingCartDetail != null) {
            existingCartDetail.setQuantityItem(existingCartDetail.getQuantityItem() + quantity);
            return cartDetailsRepository.save(existingCartDetail);
        } else {
            CartDetails cartDetail = new CartDetails();
            cartDetail.setUser(user);
            cartDetail.setProductDetails(productDetail);
            cartDetail.setQuantityItem(quantity);
            cartDetail.setTabId(tabId);
            return cartDetailsRepository.save(cartDetail);
        }
    }

    @Transactional
    public void removeProductFromOfflineCart(Long userId, Long productDetailId, Integer tabId) {
        CartDetails cartDetail = cartDetailsRepository.findByUserUserIdAndTabId(userId, tabId)
                .stream()
                .filter(cd -> cd.getProductDetails().getProductDetailId().equals(productDetailId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng."));
        cartDetailsRepository.delete(cartDetail);
    }

    public List<CartDetails> getOfflineCartDetails(Long userId, Integer tabId) {
        return (tabId == null) ? cartDetailsRepository.findByUserUserId(userId)
                : cartDetailsRepository.findByUserUserIdAndTabId(userId, tabId);
    }

    @Transactional
    public void deleteByUserIdAndTabId(Long userId, Integer tabId) {
        cartDetailsRepository.deleteByUserIdAndTabId(userId, tabId);
    }

    public PointInfoDTO getPointsByPhone(String phone) {
        Point point = pointRepository.findByPhone(phone).orElse(null);
        return point == null ? new PointInfoDTO("Khách vãng lai", 0) : new PointInfoDTO(point.getName(), point.getTotalPoint());
    }
}