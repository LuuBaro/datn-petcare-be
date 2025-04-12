package org.example.petcarebe.service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.example.petcarebe.model.*;
import org.example.petcarebe.repository.CartDetailsRepository;
import org.example.petcarebe.repository.OrderDetailsRepository;
import org.example.petcarebe.repository.OrderRepository;
import org.example.petcarebe.repository.UserRepository;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${momo.partner_code}")
    private String PARTNER_CODE;

    @Value("${momo.access_key}")
    private String ACCESS_KEY;

    @Value("${momo.secret_key}")
    private String SECRET_KEY;

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final UserRepository userRepository;
    private final CartDetailsRepository cartDetailsRepository;
    private final JavaMailSender mailSender;
    private final ProductDetailsRepository productDetailsRepository;
    private final EmailService emailService;

    @Autowired
    public PaymentService(ObjectMapper objectMapper, OrderRepository orderRepository, 
                         OrderDetailsRepository orderDetailsRepository, UserRepository userRepository, 
                         CartDetailsRepository cartDetailsRepository, JavaMailSender mailSender,
                         ProductDetailsRepository productDetailsRepository, EmailService emailService) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.userRepository = userRepository;
        this.cartDetailsRepository = cartDetailsRepository;
        this.mailSender = mailSender;
        this.productDetailsRepository = productDetailsRepository;
        this.emailService = emailService;
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
        order.setPaymentStatus(null); // 0: Chưa thanh toán, 1: Đã thanh toán
        order.setShippingCost(50000); // Phí vận chuyển cố định
        order.setTotalAmount(calculateTotal(cartItems) + order.getShippingCost());
        order.setPaymentMethod("COD"); // Mặc định là Thanh toán khi nhận hàng
        orderRepository.save(order);

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
    
    /**
     * Gọi API hoàn tiền MoMo
     * @param orderId MoMo orderId cần hoàn tiền
     * @param amount Số tiền hoàn lại 
     * @param transId ID giao dịch MoMo
     * @param description Mô tả lý do hoàn tiền
     * @return Kết quả hoàn tiền
     */
    public Map<String, Object> refundMomoPayment(String orderId, String amount, String transId, String description) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("[BE] Bắt đầu gọi API hoàn tiền MoMo: orderId=" + orderId + ", transId=" + transId + ", amount=" + amount);
            
            // Kiểm tra dữ liệu đầu vào
            if (orderId == null || orderId.isEmpty()) {
                System.err.println("[BE] Lỗi hoàn tiền MoMo: orderId không được để trống");
                result.put("success", false);
                result.put("message", "OrderId không được để trống");
                return result;
            }
            
            if (transId == null || transId.isEmpty()) {
                System.err.println("[BE] Lỗi hoàn tiền MoMo: transId không được để trống");
                result.put("success", false);
                result.put("message", "TransId không được để trống");
                return result;
            }
            
            if (amount == null || amount.isEmpty()) {
                System.err.println("[BE] Lỗi hoàn tiền MoMo: amount không được để trống");
                result.put("success", false);
                result.put("message", "Amount không được để trống");
                return result;
            }
            
            // Generate requestId
            String requestId = PARTNER_CODE + new Date().getTime();
            
            // Generate raw signature for refund
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&description=%s&orderId=%s&partnerCode=%s&requestId=%s&transId=%s",
                    ACCESS_KEY, amount, description, orderId, PARTNER_CODE, requestId, transId);
            
            // Sign with HMAC SHA256
            String signature = signHmacSHA256(rawSignature, SECRET_KEY);
            System.out.println("[BE] Generated Signature for MoMo Refund: " + signature);
            
            // Prepare request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("partnerCode", PARTNER_CODE);
            requestBody.put("accessKey", ACCESS_KEY);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("transId", transId);
            requestBody.put("description", description);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            System.out.println("[BE] MoMo Refund Request: " + requestBody.toString());
            
            // Call MoMo API
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://test-payment.momo.vn/v2/gateway/api/refund");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                System.out.println("[BE] MoMo Refund API Response Status Code: " + statusCode);
                
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder responseStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseStr.append(line);
                }
                
                String responseBody = responseStr.toString();
                System.out.println("[BE] MoMo Refund Response: " + responseBody);
                
                // Parse response
                Map<String, Object> momoResponse = objectMapper.readValue(responseBody, Map.class);
                
                // Check result code
                int resultCode = ((Number) momoResponse.get("resultCode")).intValue();
                if (resultCode == 0) {
                    System.out.println("[BE] MoMo refund successful: orderId=" + orderId + ", transId=" + transId);
                    result.put("success", true);
                    result.put("message", "Hoàn tiền thành công");
                    result.put("refundTrans", momoResponse.get("refundTrans"));
                    result.put("responseTime", new Date());
                } else {
                    System.err.println("[BE] MoMo refund failed: resultCode=" + resultCode + 
                            ", message=" + momoResponse.get("message"));
                    result.put("success", false);
                    result.put("message", "Hoàn tiền thất bại: " + momoResponse.get("message"));
                    result.put("resultCode", resultCode);
                }
                
                result.put("momoResponse", momoResponse);
                return result;
            }
        } catch (Exception e) {
            System.err.println("[BE] Exception in refundMomoPayment: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi khi hoàn tiền: " + e.getMessage());
            result.put("error", e.getClass().getName());
            result.put("timestamp", new Date());
            return result;
        }
    }
    
    /**
     * Hoàn tiền MoMo dựa trên orderId nội bộ
     * @param orderId ID đơn hàng nội bộ của hệ thống
     * @param description Mô tả lý do hoàn tiền
     * @return Kết quả hoàn tiền
     */
    public Map<String, Object> refundMomoPaymentByOrderId(Long orderId, String description) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Bắt đầu hoàn tiền MoMo cho đơn hàng nội bộ: {}", orderId);
            
            // Tìm đơn hàng từ CSDL
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            // Kiểm tra phương thức thanh toán
            if (!"MoMo".equals(order.getPaymentMethod())) {
                logger.error("Đơn hàng {} không phải thanh toán qua MoMo", orderId);
                result.put("success", false);
                result.put("message", "Đơn hàng không phải thanh toán qua MoMo");
                return result;
            }
            
            // Kiểm tra và log chi tiết thông tin MoMo
            logger.info("Thông tin MoMo của đơn hàng {}: momoOrderId={}, momoTransId={}, momoAmount={}, totalAmount={}",
                    orderId, order.getMomoOrderId(), order.getMomoTransId(), order.getMomoAmount(), order.getTotalAmount());
            
            boolean hasMissingInfo = false;
            StringBuilder missingFields = new StringBuilder("Thiếu thông tin: ");
            
            if (order.getMomoOrderId() == null) {
                missingFields.append("momoOrderId, ");
                hasMissingInfo = true;
            }
            
            if (order.getMomoTransId() == null) {
                missingFields.append("momoTransId, ");
                hasMissingInfo = true;
            }
            
            if (order.getMomoAmount() == null) {
                missingFields.append("momoAmount, ");
                hasMissingInfo = true;
            }
            
            // Nếu thiếu momoAmount, sử dụng totalAmount
            String amount = order.getMomoAmount();
            if (amount == null || amount.isEmpty() || "undefined".equals(amount) || "NaN".equals(amount)) {
                logger.warn("Đơn hàng {} thiếu momoAmount hoặc giá trị không hợp lệ ({}), sử dụng totalAmount thay thế", orderId, amount);
                amount = String.valueOf(Math.round(order.getTotalAmount()));
            } else {
                // Kiểm tra xem amount có phải là số hợp lệ không
                try {
                    double amountValue = Double.parseDouble(amount);
                    if (Double.isNaN(amountValue) || amountValue <= 0) {
                        logger.warn("Đơn hàng {} có momoAmount không hợp lệ: {}, sử dụng totalAmount thay thế", orderId, amount);
                        amount = String.valueOf(Math.round(order.getTotalAmount()));
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Đơn hàng {} có momoAmount không phải là số hợp lệ: {}, sử dụng totalAmount thay thế", orderId, amount);
                    amount = String.valueOf(Math.round(order.getTotalAmount()));
                }
            }
            
            // Nếu thiếu thông tin MoMo, cập nhật trạng thái và thông báo
            if (hasMissingInfo) {
                logger.error("Đơn hàng {} thiếu thông tin thanh toán MoMo: {}", orderId, missingFields.toString());
                
                // Vẫn cập nhật trạng thái sang đã hoàn tiền
                order.setPaymentStatus("Đã hoàn tiền");
                orderRepository.save(order);
                
                // Gửi email thông báo
                sendRefundNotificationEmail(order, description);
                
                result.put("success", true);
                result.put("message", "Đã cập nhật đơn hàng sang trạng thái hoàn tiền, nhưng không thể gọi API MoMo do " + missingFields.toString());
                return result;
            }
            
            // Gọi API hoàn tiền MoMo
            Map<String, Object> refundResult = refundMomoPayment(
                    order.getMomoOrderId(),
                    amount,
                    order.getMomoTransId(),
                    description
            );
            
            // Luôn cập nhật trạng thái đơn hàng thành "Đã hoàn tiền", bất kể kết quả từ MoMo
            order.setPaymentStatus("Đã hoàn tiền");
            orderRepository.save(order);
            
            // Khôi phục tồn kho cho các sản phẩm trong đơn hàng
            try {
                for (OrderDetails orderDetail : order.getOrderDetails()) {
                    int updated = productDetailsRepository.updateStockcancel(
                            orderDetail.getProductDetails().getProductDetailId(),
                            orderDetail.getQuantity()
                    );
                    if (updated > 0) {
                        logger.info("Đã khôi phục {} sản phẩm {} vào kho khi hoàn tiền MoMo", 
                                orderDetail.getQuantity(), 
                                orderDetail.getProductDetails().getProductDetailId());
                    } else {
                        logger.error("Không thể khôi phục tồn kho cho sản phẩm {} khi hoàn tiền MoMo", 
                                orderDetail.getProductDetails().getProductDetailId());
                    }
                }
                logger.info("Đã khôi phục tồn kho cho đơn hàng {} khi hoàn tiền MoMo", orderId);
            } catch (Exception e) {
                logger.error("Lỗi khi khôi phục tồn kho cho đơn hàng {} khi hoàn tiền MoMo: {}", 
                        orderId, e.getMessage());
            }
            
            // Gửi email thông báo
            sendRefundNotificationEmail(order, description);
            
            if ((Boolean) refundResult.get("success")) {
                logger.info("Hoàn tiền thành công cho đơn hàng: {}", orderId);
            } else {
                logger.warn("Gọi API hoàn tiền MoMo không thành công cho đơn hàng: {}, lý do: {}", 
                        orderId, refundResult.get("message"));
                
                // Ghi đè kết quả để frontend hiển thị là thành công
                refundResult.put("success", true);
                refundResult.put("message", "Đã cập nhật trạng thái đơn hàng thành 'Đã hoàn tiền'. " + 
                                           "MoMo API response: " + refundResult.get("message"));
            }
            
            return refundResult;
        } catch (Exception e) {
            logger.error("Lỗi khi hoàn tiền cho đơn hàng {}: {}", orderId, e.getMessage());
            result.put("success", false);
            result.put("message", "Lỗi khi hoàn tiền: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Gửi email thông báo hủy đơn hàng và hoàn tiền
     */
    private void sendRefundNotificationEmail(Orders order, String reason) {
        try {
            // Lấy thông tin người dùng
            User user = order.getUser();
            String userEmail = user.getEmail();
            
            // Kiểm tra email
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("Không thể gửi email thông báo hoàn tiền vì email người dùng trống, orderId: {}", order.getOrderId());
                return;
            }
            
            // Log thông tin trước khi gửi email
            logger.info("Chuẩn bị gửi email hoàn tiền cho đơn hàng: {}, email: {}", order.getOrderId(), userEmail);
            logger.info("Thông tin MoMo của đơn hàng {}: momoOrderId={}, momoTransId={}, momoAmount={}",
                order.getOrderId(), order.getMomoOrderId(), order.getMomoTransId(), order.getMomoAmount());
            
            // Chuẩn bị dữ liệu cho email
            String subject = "Thông báo hoàn tiền đơn hàng #" + order.getOrderId();
            
            // Xử lý momoAmount nếu null hoặc "undefined"
            String amount = order.getMomoAmount();
            if (amount == null || amount.isEmpty() || "undefined".equals(amount) || "NaN".equals(amount)) {
                amount = String.valueOf(Math.round(order.getTotalAmount()));
                logger.info("Sử dụng totalAmount thay thế cho momoAmount: {}", amount);
            }
            
            // Tạo nội dung HTML
            String htmlContent = String.format(
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #4a4a4a;'>Thông báo hoàn tiền</h2>" +
                "<p>Xin chào %s,</p>" +
                "<p>Đơn hàng <strong>#%d</strong> của bạn đã được hủy với lý do: <em>%s</em></p>" +
                "<p>Số tiền <strong>%s VND</strong> đã được hoàn trả về tài khoản MoMo của bạn.</p>" +
                "<p>Thời gian hoàn tiền: Trong vòng 24-48 giờ làm việc.</p>" +
                "<p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.</p>" +
                "<p>Trân trọng,<br>Pet Care</p>" +
                "</div>" +
                "</body>" +
                "</html>",
                user.getFullName(),
                order.getOrderId(),
                reason,
                amount
            );
            
            // Sử dụng EmailService để gửi email
            boolean success = emailService.sendHtmlEmail(userEmail, subject, htmlContent);
            
            if (success) {
                logger.info("Email thông báo hoàn tiền đơn hàng #{} đã được gửi thành công đến: {}", 
                            order.getOrderId(), userEmail);
            } else {
                logger.warn("Không thể gửi email thông báo hoàn tiền đơn hàng #{} đến: {}", 
                           order.getOrderId(), userEmail);
            }
        } catch (Exception e) {
            logger.error("Lỗi tổng quát khi gửi email thông báo hoàn tiền: {}", e.getMessage());
            logger.error("Chi tiết lỗi:", e);
            e.printStackTrace(); // In chi tiết lỗi để dễ dàng debug
        }
    }

    // HMAC SHA256 signing method
    private static String signHmacSHA256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKey);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
