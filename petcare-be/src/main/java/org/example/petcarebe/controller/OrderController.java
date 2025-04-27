package org.example.petcarebe.controller;

import org.example.petcarebe.dto.OrderDTO;
import org.example.petcarebe.dto.request.CheckoutRequestDTO;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.service.CartDetailsService;
import org.example.petcarebe.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartDetailsService cartDetailsService;

    @GetMapping("/check-exists/{orderId}")
    public ResponseEntity<Map<String, Object>> checkOrderExists(@PathVariable Long orderId) {
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
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Long statusId = request.get("statusId") != null ? 
                    Long.valueOf(request.get("statusId").toString()) : null;
            
            String paymentStatus = request.get("paymentStatus") != null ? 
                    request.get("paymentStatus").toString() : null;
            
            String cancelReason = request.get("cancelReason") != null ?
                    request.get("cancelReason").toString() : null;
            
            logger.info("Updating order status: orderId={}, statusId={}, paymentStatus={}, cancelReason={}",
                    orderId, statusId, paymentStatus, cancelReason);
            
            Orders order = orderService.updateOrderStatusAndPayment(orderId, statusId, paymentStatus);
            
            // Nếu có lý do hủy đơn, lưu lại
            if (cancelReason != null && !cancelReason.isEmpty() && statusId != null && statusId == 5) {
                orderService.addCancellationReason(orderId, cancelReason);
                logger.info("Added cancellation reason for orderId={}: {}", orderId, cancelReason);
            }
            
            response.put("message", "Cập nhật trạng thái đơn hàng thành công!");
            response.put("orderId", order.getOrderId());
            response.put("newStatusId", order.getStatusOrder().getStatusId());
            response.put("newStatusName", order.getStatusOrder().getStatusName());
            response.put("paymentStatus", order.getPaymentStatus());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating order status for orderId={}: {}", orderId, e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{orderId}/payment-status")
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

    @GetMapping("/check-momo-order")
    public ResponseEntity<Map<String, Object>> checkMomoOrderExists(@RequestParam String momoOrderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = orderService.checkMomoOrderExists(momoOrderId);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi kiểm tra đơn hàng MoMo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> updateOrderMomoInfo(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String momoOrderId = request.get("momoOrderId");
            String momoTransId = request.get("momoTransId");
            
            logger.info("Updating MoMo info for orderId={}: momoOrderId={}, momoTransId={}", 
                    orderId, momoOrderId, momoTransId);
            
            Orders order = orderService.updateMomoInfo(orderId, momoOrderId, momoTransId);
            
            response.put("message", "Cập nhật thông tin thanh toán MoMo thành công");
            response.put("orderId", order.getOrderId());
            response.put("momoOrderId", order.getMomoOrderId());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating MoMo info for orderId={}: {}", orderId, e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Server error when updating MoMo info for orderId={}: {}", orderId, e.getMessage());
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API để sửa lỗi các đơn hàng có momoAmount là "undefined"
    @PostMapping("/fix-undefined-momo-amount")
    public ResponseEntity<Map<String, Object>> fixUndefinedMomoAmount() {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Bắt đầu xử lý yêu cầu sửa lỗi momoAmount 'undefined' hoặc 'NaN'");
            
            // Lấy tất cả đơn hàng thanh toán qua MoMo
            List<Orders> momoOrders = orderService.findByPaymentMethod("MoMo");
            
            int fixedCount = 0;
            List<Long> fixedOrderIds = new ArrayList<>();
            
            for (Orders order : momoOrders) {
                String momoAmount = order.getMomoAmount();
                
                // Kiểm tra nếu momoAmount là undefined, NaN hoặc không phải là số hợp lệ
                boolean needsFix = momoAmount == null || 
                                 momoAmount.isEmpty() || 
                                 "undefined".equals(momoAmount) ||
                                 "NaN".equals(momoAmount);
                
                // Kiểm tra thêm nếu là chuỗi số hợp lệ
                if (!needsFix) {
                    try {
                        double amount = Double.parseDouble(momoAmount);
                        if (Double.isNaN(amount) || amount <= 0) {
                            needsFix = true;
                        }
                    } catch (NumberFormatException e) {
                        // Không phải số hợp lệ
                        needsFix = true;
                    }
                }
                
                if (needsFix) {
                    // Sử dụng totalAmount thay thế
                    String newAmount = String.valueOf(Math.round(order.getTotalAmount()));
                    order.setMomoAmount(newAmount);
                    
                    // Lưu đơn hàng
                    orderService.save(order);
                    
                    logger.info("Đã sửa momoAmount từ '{}' thành '{}' cho đơn hàng {}", 
                            momoAmount, newAmount, order.getOrderId());
                    
                    fixedCount++;
                    fixedOrderIds.add(order.getOrderId());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã sửa thành công " + fixedCount + " đơn hàng có momoAmount không hợp lệ");
            response.put("fixedCount", fixedCount);
            response.put("fixedOrderIds", fixedOrderIds);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi sửa momoAmount: " + e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Lỗi khi sửa momoAmount: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            OrderDTO orderDTO = orderService.getOrderDTOById(orderId);
            return ResponseEntity.ok(orderDTO);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

}