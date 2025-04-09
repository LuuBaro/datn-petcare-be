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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private final OrderRepository ordersRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final UserRepository userRepository;
    private final CartDetailsRepository cartDetailsRepository;
    
    // MoMo API credentials - shared với MomoService
    private static final String PARTNER_CODE = "MOMO";
    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        order.setPaymentStatus(null); // 0: Chưa thanh toán, 1: Đã thanh toán
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
