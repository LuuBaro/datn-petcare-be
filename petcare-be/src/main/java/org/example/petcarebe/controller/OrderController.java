package org.example.petcarebe.controller;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.CartDetailsService;
import org.example.petcarebe.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartDetailsService cartDetailsService;

    @GetMapping("/check-exists/{orderId}")
    public ResponseEntity<Map<String, Object>> checkOrderExists(@PathVariable String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = orderService.checkOrderExists(orderId);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi kiểm tra đơn hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequestDTO request) {
        System.out.println("Received checkout request: " + request);
        Map<String, Object> response = new HashMap<>();
        try {
            // Kiểm tra dữ liệu đầu vào
            if (request.getUserId() == null || request.getPaymentMethod() == null) {
                response.put("message", "Thiếu thông tin người dùng hoặc phương thức thanh toán");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Kiểm tra xem đơn hàng đã tồn tại chưa
            if (request.getOrderId() != null) {
                boolean exists = orderService.checkOrderExists(request.getOrderId());
                if (exists) {
                    response.put("message", "Đơn hàng đã tồn tại");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
            }

            // Gọi OrderService để tạo đơn hàng
            Orders order = orderService.checkout(request);

            // Trả về response với thông tin đơn hàng
            response.put("message", "Đặt hàng thành công");
            response.put("orderId", order.getOrderId());
            response.put("paymentStatus", order.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/clearCart/{productDetailId}")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable Long productDetailId) {
        cartDetailsService.deleteCartDetails(productDetailId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa sản phẩm khỏi giỏ hàng thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orderDTOList = orderService.getAllOrders();
        return ResponseEntity.ok(orderDTOList);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Kiểm tra xem đơn hàng đã bị hủy chưa
            Orders existingOrder = orderService.getOrderById(orderId);
            if (existingOrder.getStatusOrder().getStatusId() == 5) {
                response.put("message", "Đơn hàng này đã được hủy trước đó");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                response.put("message", "Lý do hủy không được để trống");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Hủy đơn hàng và cập nhật trạng thái thanh toán
            Orders order = orderService.cancelOrder(orderId, reason);
            
            response.put("message", "Đơn hàng đã được hủy thành công");
            response.put("orderId", order.getOrderId());
            response.put("status", order.getStatusOrder().getStatusName());
            response.put("paymentStatus", order.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> requestBody
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Lấy các tham số từ request body
            Long statusId = requestBody.get("statusId") != null ? 
                Long.parseLong(requestBody.get("statusId").toString()) : null;
            String paymentStatus = requestBody.get("paymentStatus") != null ? 
                requestBody.get("paymentStatus").toString() : null;

            // Kiểm tra dữ liệu đầu vào
            if (statusId == null && paymentStatus == null) {
                response.put("message", "Cần cung cấp statusId hoặc paymentStatus");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Cập nhật trạng thái đơn hàng
            Orders updatedOrder = orderService.updateOrderStatusAndPayment(orderId, statusId, paymentStatus);

            response.put("message", "Cập nhật trạng thái đơn hàng thành công");
            response.put("orderId", updatedOrder.getOrderId());
            response.put("statusId", updatedOrder.getStatusOrder().getStatusId());
            response.put("paymentStatus", updatedOrder.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> requestBody
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String paymentStatus = requestBody.get("paymentStatus");
            if (paymentStatus == null) {
                response.put("message", "paymentStatus is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            Orders order = orderService.updatePaymentStatus(orderId, paymentStatus);
            response.put("message", "Cập nhật trạng thái thanh toán thành công");
            response.put("orderId", order.getOrderId());
            response.put("paymentStatus", order.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint lấy đơn hàng "ORDER ONLINE" với statusId = 4
    @GetMapping("/completed-online")
    public ResponseEntity<List<OrderDTO>> getCompletedOnlineOrders() {
        List<OrderDTO> completedOnlineOrders = orderService.getCompletedOnlineOrders();
        return ResponseEntity.ok(completedOnlineOrders);
    }

    // Endpoint tìm đơn hàng online theo khoảng thời gian
    @GetMapping("/online-by-date-range")
    public ResponseEntity<?> getOnlineOrdersByDateRange(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            List<OrderDTO> onlineOrders = orderService.getOnlineOrdersByDateRange(startDate, endDate);
            return ResponseEntity.ok(onlineOrders);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Định dạng ngày không hợp lệ hoặc lỗi xử lý: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    @GetMapping("/by-voucher/{voucherId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByVoucherId(@PathVariable Long voucherId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByVoucherId(voucherId);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.emptyList()); // Trả về danh sách rỗng nếu có lỗi
        }
    }

    // Endpoint để hủy đơn hàng khi quá trình tạo thanh toán thất bại
    @DeleteMapping("/{orderId}/payment-failed")
    public ResponseEntity<Map<String, Object>> cancelOrderOnPaymentFailure(@PathVariable Long orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Tìm đơn hàng
            Orders order = orderService.cancelOrder(orderId, "Quá trình tạo thanh toán thất bại");
            response.put("message", "Đã hủy đơn hàng do quá trình tạo thanh toán thất bại");
            response.put("orderId", order.getOrderId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}