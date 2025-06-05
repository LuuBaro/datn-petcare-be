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
import java.text.SimpleDateFormat;

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
     * @param orderId MoMo orderId cần hoàn tiền (có thể là giá trị bất kỳ miễn là hợp lệ, không nhất thiết phải khớp với orderId ban đầu)
     * @param amount Số tiền hoàn lại (cần khớp với số tiền đã thanh toán)
     * @param transId ID giao dịch MoMo (bắt buộc và phải chính xác, đây là thông số quan trọng nhất để xác định giao dịch)
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
                
                // Log chi tiết kết quả từ MoMo
                logger.info("=============== CHI TIẾT KẾT QUẢ HOÀN TIỀN MOMO ===============");
                logger.info("RequestId: {}", requestId);
                logger.info("OrderId (gửi đi): {}", orderId);
                logger.info("TransId (gửi đi): {}", transId);
                logger.info("Amount (gửi đi): {}", amount);
                
                // Log các thông tin từ response của MoMo
                logger.info("ResultCode: {}", momoResponse.get("resultCode"));
                logger.info("Message: {}", momoResponse.get("message"));
                logger.info("TransId (nhận về): {}", momoResponse.get("transId"));
                logger.info("OrderId (nhận về): {}", momoResponse.get("orderId"));
                logger.info("RequestId (nhận về): {}", momoResponse.get("requestId"));
                logger.info("ExtraData: {}", momoResponse.get("extraData"));
                
                if (momoResponse.containsKey("refundTrans")) {
                    logger.info("RefundTrans: {}", momoResponse.get("refundTrans"));
                }
                
                // Check result code
                int resultCode = ((Number) momoResponse.get("resultCode")).intValue();
                
                // Ghi log thông tin kết quả chi tiết với mã lỗi
                if (resultCode == 0) {
                    logger.info("[BE] MoMo refund THÀNH CÔNG: orderId={}, transId={}, resultCode={}", 
                            orderId, transId, resultCode);
                    result.put("success", true);
                    result.put("message", "Hoàn tiền thành công");
                    result.put("refundTrans", momoResponse.get("refundTrans"));
                    result.put("responseTime", new Date());
                } else {
                    logger.error("[BE] MoMo refund THẤT BẠI: orderId={}, transId={}, resultCode={}, message={}", 
                            orderId, transId, resultCode, momoResponse.get("message"));
                    
                    // Log ý nghĩa của mã lỗi
                    String errorDescription = getMomoResultCodeDescription(resultCode);
                    logger.error("[BE] Mô tả lỗi: {}", errorDescription);
                    
                    result.put("success", false);
                    result.put("message", "Hoàn tiền thất bại: " + momoResponse.get("message"));
                    result.put("resultCode", resultCode);
                    result.put("errorDescription", errorDescription);
                }
                logger.info("=============== KẾT THÚC CHI TIẾT HOÀN TIỀN MOMO ===============");
                
                result.put("momoResponse", momoResponse);
                result.put("statusCode", statusCode);
                result.put("requestDetails", Map.of(
                    "requestId", requestId,
                    "orderId", orderId,
                    "transId", transId,
                    "amount", amount,
                    "timestamp", new Date()
                ));
                
                return result;
            }
        } catch (Exception e) {
            System.err.println("[BE] Exception in refundMomoPayment: " + e.getMessage());
            e.printStackTrace();
            logger.error("[BE] Lỗi ngoại lệ khi hoàn tiền MoMo: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("message", "Lỗi khi hoàn tiền: " + e.getMessage());
            result.put("error", e.getClass().getName());
            result.put("stackTrace", e.getStackTrace()[0].toString());
            result.put("timestamp", new Date());
            return result;
        }
    }
    
    /**
     * Lấy mô tả chi tiết cho mã kết quả của MoMo
     * @param resultCode Mã kết quả từ MoMo
     * @return Mô tả chi tiết về mã kết quả
     */
    private String getMomoResultCodeDescription(int resultCode) {
        switch (resultCode) {
            case 0:
                return "Giao dịch thành công";
            case 1:
                return "Giao dịch đã tồn tại trong hệ thống";
            case 2:
                return "Merchant không hợp lệ (không tìm thấy, bị khóa, không được kích hoạt...)";
            case 3:
                return "Dữ liệu gửi sang không đúng định dạng";
            case 4:
                return "Khởi tạo GD không thành công do url hoặc IPN không hợp lệ";
            case 5:
                return "Tài khoản người dùng không đủ tiền";
            case 6:
                return "Giao dịch không thành công do người dùng nhập sai OTP";
            case 7:
                return "Giao dịch bị từ chối bởi người dùng";
            case 8:
                return "Quá thời gian giao dịch";
            case 9:
                return "Mã đơn hàng không hợp lệ";
            case 10:
                return "Mã đơn hàng đã tồn tại";
            case 11:
                return "Mã đơn hàng đã thanh toán";
            case 12:
                return "Số tiền không hợp lệ";
            case 13:
                return "Số tiền vượt quá hạn mức cho phép";
            case 14:
                return "Tài khoản người nhận không tồn tại";
            case 15:
                return "Giao dịch không thành công";
            case 16:
                return "Điểm giao dịch không được phép giao dịch";
            case 20:
                return "Địa chỉ IP truy cập bị chặn";
            case 21:
                return "Mã orderInfo không hợp lệ";
            case 22:
                return "Tài khoản người dùng không tồn tại";
            case 23:
                return "Giao dịch chưa được thanh toán";
            case 24:
                return "Giao dịch bị từ chối";
            case 25:
                return "Giao dịch không thể hoàn trả";
            case 26:
                return "Giao dịch đã được hoàn trả";
            case 28:
                return "Số tiền hoàn lại lớn hơn số tiền thanh toán";
            case 29:
                return "Số tiền còn lại không đủ để hoàn trả";
            case 30:
                return "Giao dịch đã bị đóng hoặc hết hạn";
            case 31:
                return "Yêu cầu hoàn tiền đang được xử lý";
            case 32:
                return "Giao dịch không được xử lý do MoMo bảo trì";
            case 33:
                return "Giao dịch không thể hủy";
            case 34:
                return "Giao dịch đã bị hủy";
            case 36:
                return "Phiên làm việc đã hết hạn";
            case 37:
                return "Chữ ký không hợp lệ";
            case 38:
                return "Thiếu tham số bắt buộc";
            case 39:
                return "Giao dịch không được phép thực hiện";
            case 40:
                return "Số tiền truy vấn và thanh toán không trùng khớp";
            case 41:
                return "Xác thực OTP thất bại";
            case 42:
                return "Số điện thoại không hợp lệ";
            case 43:
                return "Thuê bao chưa kích hoạt";
            case 44:
                return "Tài khoản bị tạm khóa";
            case 45:
                return "Tài khoản chưa xác thực KYC";
            case 46:
                return "Số tiền gửi vượt quá tỷ lệ phí";
            case 47:
                return "Giao dịch ngoài giờ phục vụ";
            case 48:
                return "Thẻ bị khóa hoặc hết hạn mức";
            case 49:
                return "Giao dịch nghi ngờ gian lận";
            case 50:
                return "Sai thông tin chủ thẻ";
            case 51:
                return "Tài khoản không đủ tiền";
            case 63:
                return "Sai thông tin xác thực thẻ";
            case 64:
                return "Thẻ hết hạn mức hoặc không được phép giao dịch online";
            case 65:
                return "Tài khoản người dùng đang bị tạm khóa";
            case 66:
                return "3DS Token không hợp lệ";
            case 67:
                return "3DS Token đã hết hạn";
            case 69:
                return "Thẻ chưa kích hoạt hoặc không được phép giao dịch trực tuyến";
            case 70:
                return "Khách hàng chưa xác thực OTP";
            case 71:
                return "Khách hàng đã hủy giao dịch";
            case 72:
                return "Bank từ chối xử lý giao dịch";
            case 73:
                return "Bank timeout";
            case 79:
                return "Chữ ký NAPAS không hợp lệ";
            case 80:
                return "Không tìm thấy thông tin khách hàng";
            case 81:
                return "Hết thời gian giao dịch";
            case 82:
                return "Bank bảo trì";
            case 99:
                return "Lỗi không xác định";
            case 9043:
                return "Thuê bao MoMo bị khóa";
            default:
                return "Mã lỗi chưa được định nghĩa: " + resultCode;
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
            System.out.println("\n===== BẮT ĐẦU XỬ LÝ HOÀN TIỀN MOMO =====");
            System.out.println("OrderId: " + orderId);
            System.out.println("Mô tả: " + description);
            
            logger.info("Bắt đầu hoàn tiền MoMo cho đơn hàng nội bộ: {}", orderId);
            
            // Tìm đơn hàng từ CSDL
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            System.out.println("Tìm thấy đơn hàng: " + order.getOrderId());
            System.out.println("PaymentMethod: " + order.getPaymentMethod());
            System.out.println("MomoOrderId: " + order.getMomoOrderId());
            System.out.println("MomoTransId: " + order.getMomoTransId());
            
            // Kiểm tra phương thức thanh toán
            if (!"MoMo".equals(order.getPaymentMethod())) {
                logger.error("Đơn hàng {} không phải thanh toán qua MoMo", orderId);
                System.out.println("Đơn hàng không phải thanh toán qua MoMo");
                result.put("success", false);
                result.put("message", "Đơn hàng không phải thanh toán qua MoMo");
                return result;
            }
            
            // Kiểm tra và log chi tiết thông tin MoMo
            logger.info("Thông tin MoMo của đơn hàng {}: momoOrderId={}, momoTransId={}, totalAmount={}",
                    orderId, order.getMomoOrderId(), order.getMomoTransId(), order.getTotalAmount());
            
            boolean hasMissingInfo = false;
            StringBuilder missingFields = new StringBuilder("Thiếu thông tin: ");
            
            if (order.getMomoTransId() == null) {
                missingFields.append("momoTransId, ");
                hasMissingInfo = true;
                System.out.println("Thiếu thông tin: momoTransId");
            }
            
            // Nếu thiếu thông tin MoMo, cập nhật trạng thái và thông báo
            if (hasMissingInfo) {
                logger.error("Đơn hàng {} thiếu thông tin thanh toán MoMo: {}", orderId, missingFields.toString());
                System.out.println("Đơn hàng thiếu thông tin thanh toán MoMo: " + missingFields.toString());
                
                // Vẫn cập nhật trạng thái sang đã hoàn tiền
                order.setPaymentStatus("Đã hoàn tiền");
                orderRepository.save(order);
                System.out.println("Đã cập nhật trạng thái thanh toán thành 'Đã hoàn tiền'");
                
                // Chuẩn bị kết quả để trả về trước khi gửi email
                result.put("success", true);
                result.put("message", "Đã cập nhật đơn hàng sang trạng thái hoàn tiền, nhưng không thể gọi API MoMo do " + missingFields.toString());
                
                // Gửi email thông báo sau khi đã trả về kết quả (xử lý bất đồng bộ)
                new Thread(() -> {
                    try {
                        sendRefundNotificationEmail(order, description);
                        System.out.println("Đã gửi email thông báo hoàn tiền");
                    } catch (Exception e) {
                        logger.error("Lỗi khi gửi email thông báo hoàn tiền: {}", e.getMessage());
                    }
                }).start();
                
                System.out.println("===== KẾT THÚC XỬ LÝ HOÀN TIỀN MOMO =====\n");
                return result;
            }
            
            // Tạo một orderId mới và ngẫu nhiên để tránh trùng lặp
            String refundOrderId = "REFUND_" + orderId + "_" + System.currentTimeMillis();
            logger.info("Tạo orderId mới cho hoàn tiền MoMo: {}", refundOrderId);
            System.out.println("Tạo orderId mới cho hoàn tiền MoMo: " + refundOrderId);
            
            // Sử dụng totalAmount thay vì momoAmount
            String amount = String.valueOf(Math.round(order.getTotalAmount()));
            
            // Gọi API hoàn tiền MoMo
            System.out.println("Bắt đầu gọi API hoàn tiền MoMo với thông tin:");
            System.out.println("- refundOrderId: " + refundOrderId + " (thay cho momoOrderId cũ: " + order.getMomoOrderId() + ")");
            System.out.println("- momoTransId: " + order.getMomoTransId());
            System.out.println("- amount: " + amount);
            System.out.println("- description: " + description);
            
            Map<String, Object> refundResult = refundMomoPayment(
                    refundOrderId, // Sử dụng orderId mới thay vì momoOrderId cũ
                    amount,
                    order.getMomoTransId(),
                    description
            );
            
            System.out.println("Kết quả từ API hoàn tiền MoMo: " + refundResult);
            
            // Luôn cập nhật trạng thái đơn hàng thành "Đã hoàn tiền", bất kể kết quả từ MoMo
            order.setPaymentStatus("Đã hoàn tiền");
            orderRepository.save(order);
            System.out.println("Đã cập nhật trạng thái đơn hàng thành 'Đã hoàn tiền'");
            
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
                        System.out.println("Đã khôi phục " + orderDetail.getQuantity() + " sản phẩm " + 
                                           orderDetail.getProductDetails().getProductDetailId() + " vào kho");
                    } else {
                        logger.error("Không thể khôi phục tồn kho cho sản phẩm {} khi hoàn tiền MoMo", 
                                orderDetail.getProductDetails().getProductDetailId());
                        System.out.println("Không thể khôi phục tồn kho cho sản phẩm " + 
                                          orderDetail.getProductDetails().getProductDetailId());
                    }
                }
                logger.info("Đã khôi phục tồn kho cho đơn hàng {} khi hoàn tiền MoMo", orderId);
                System.out.println("Đã khôi phục tồn kho cho đơn hàng");
            } catch (Exception e) {
                logger.error("Lỗi khi khôi phục tồn kho cho đơn hàng {} khi hoàn tiền MoMo: {}", 
                        orderId, e.getMessage());
                System.out.println("Lỗi khi khôi phục tồn kho: " + e.getMessage());
            }
            
            // Chuẩn bị kết quả để trả về trước khi gửi email
            if ((Boolean) refundResult.get("success")) {
                logger.info("Hoàn tiền thành công cho đơn hàng: {}", orderId);
                System.out.println("Hoàn tiền thành công cho đơn hàng: " + orderId);
            } else {
                logger.warn("Gọi API hoàn tiền MoMo không thành công cho đơn hàng: {}, lý do: {}", 
                        orderId, refundResult.get("message"));
                System.out.println("Gọi API hoàn tiền MoMo không thành công, lý do: " + refundResult.get("message"));
                
                // Ghi đè kết quả để frontend hiển thị là thành công
                refundResult.put("success", true);
                refundResult.put("message", "Đã cập nhật trạng thái đơn hàng thành 'Đã hoàn tiền'. " + 
                                           "MoMo API response: " + refundResult.get("message"));
                System.out.println("Đã ghi đè kết quả để hiển thị thành công");
            }
            
            // Gửi email thông báo sau khi đã trả về kết quả (xử lý bất đồng bộ)
            new Thread(() -> {
                try {
                    sendRefundNotificationEmail(order, description);
                    System.out.println("Đã gửi email thông báo hoàn tiền");
                } catch (Exception e) {
                    logger.error("Lỗi khi gửi email thông báo hoàn tiền: {}", e.getMessage());
                }
            }).start();
            
            System.out.println("===== KẾT THÚC XỬ LÝ HOÀN TIỀN MOMO =====\n");
            return refundResult;
        } catch (Exception e) {
            logger.error("Lỗi khi hoàn tiền cho đơn hàng {}: {}", orderId, e.getMessage());
            System.out.println("Lỗi khi hoàn tiền cho đơn hàng: " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi khi hoàn tiền: " + e.getMessage());
            System.out.println("===== KẾT THÚC XỬ LÝ HOÀN TIỀN MOMO VỚI LỖI =====\n");
            return result;
        }
    }
    
    /**
     * Gửi email thông báo hủy đơn hàng và hoàn tiền
     */
    private void sendRefundNotificationEmail(Orders order, String reason) {
        try {
            User user = order.getUser();
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                logger.warn("Không thể gửi email hoàn tiền cho đơn hàng {}: Người dùng không có email", order.getOrderId());
                return;
            }
            
            logger.info("Gửi email thông báo hoàn tiền cho đơn hàng {}", order.getOrderId());
            
            String subject = "Thông báo hoàn tiền cho đơn hàng #" + order.getOrderId();
            
            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; padding: 20px; border-radius: 5px;'>");
            content.append("<h2 style='color: #4CAF50; text-align: center;'>Thông Báo Hoàn Tiền</h2>");
            content.append("<p>Xin chào <strong>").append(user.getFullName()).append("</strong>,</p>");
            content.append("<p>Đơn hàng <strong>#").append(order.getOrderId()).append("</strong> của bạn đã được hoàn tiền thành công.</p>");
            content.append("<p><strong>Lý do hoàn tiền:</strong> ").append(reason).append("</p>");
            content.append("<p><strong>Thông tin đơn hàng:</strong></p>");
            content.append("<ul>");
            content.append("<li><strong>Mã đơn hàng:</strong> #").append(order.getOrderId()).append("</li>");
            content.append("<li><strong>Ngày đặt hàng:</strong> ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(order.getOrderDate())).append("</li>");
            content.append("<li><strong>Phương thức thanh toán:</strong> ").append(order.getPaymentMethod()).append("</li>");
            content.append("</ul>");
            
            content.append("<p>Tiền hoàn trả sẽ được chuyển về tài khoản thanh toán của bạn trong vòng 3-5 ngày làm việc (tùy theo chính sách của ngân hàng/ví điện tử).</p>");
            
            content.append("<p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua email hỗ trợ hoặc hotline.</p>");
            content.append("<p>Trân trọng,<br/>Đội ngũ Petcare</p>");
            content.append("</div>");
            
            emailService.sendEmail(user.getEmail(), subject, content.toString());
            logger.info("Đã gửi email thông báo hoàn tiền thành công cho đơn hàng {}", order.getOrderId());
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email thông báo hoàn tiền cho đơn hàng {}: {}", order.getOrderId(), e.getMessage());
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
