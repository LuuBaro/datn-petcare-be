package org.example.petcarebe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.repository.OrderDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${gemini.api.key}") // 🔹 Lấy API key từ file cấu hình (application.properties)
    private String geminiApiKey;

    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";

    @Autowired
    private ProductsService productsService;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String getAiResponse(String userMessage) {
        Map<String, Object> data = new HashMap<>();

        // 🔹 Xác định nhu cầu của người dùng
        if (containsBestSellingQuery(userMessage)) {
            data.put("bestSellingProducts", getBestSellingProducts());
        } else if (containsAllProductsQuery(userMessage)) {
            data.put("allProducts", productsService.getAllProductss());
        } else if (containsProductDetailQuery(userMessage)) {
            data.put("productDetails", getProductDetails(userMessage));
        } else if (containsGeneralInfoQuery(userMessage)) {
            data.put("storeInfo", getStoreInformation());
        } else {
            data.put("allProducts", productsService.getAllProductss()); // Trường hợp mặc định
        }

        // 🔹 Gửi request và nhận phản hồi từ AI
        String aiResponse = sendRequestToAI(userMessage, data);
        return formatAiResponse(aiResponse);
    }

    // 🛒 Xác định câu hỏi về sản phẩm bán chạy
    private boolean containsBestSellingQuery(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase();
        return lowerCaseMessage.contains("bán chạy nhất") || lowerCaseMessage.contains("phổ biến nhất");
    }

    // 🛍️ Xác định câu hỏi về danh sách sản phẩm
    private boolean containsAllProductsQuery(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase();
        return lowerCaseMessage.contains("tất cả sản phẩm") || lowerCaseMessage.contains("danh sách sản phẩm");
    }

    // 📦 Xác định câu hỏi về thông tin chi tiết sản phẩm
    private boolean containsProductDetailQuery(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase();
        return lowerCaseMessage.contains("thông tin sản phẩm") || lowerCaseMessage.contains("giá") || lowerCaseMessage.contains("chi tiết");
    }

    // 🏪 Xác định câu hỏi về thông tin cửa hàng
    private boolean containsGeneralInfoQuery(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase();
        return lowerCaseMessage.contains("giờ mở cửa") ||
                lowerCaseMessage.contains("chính sách") ||
                lowerCaseMessage.contains("đổi trả") ||
                lowerCaseMessage.contains("liên hệ") ||
                lowerCaseMessage.contains("số điện thoại") ||
                lowerCaseMessage.contains("email");
    }

    // 📊 Lấy danh sách sản phẩm bán chạy
    private List<Map<String, Object>> getBestSellingProducts() {
        Pageable top3 = PageRequest.of(0, 3);
        List<Object[]> bestSellingProducts = orderDetailsRepository.findBestSellingProducts(top3);

        List<Map<String, Object>> bestSellersList = new ArrayList<>();
        for (Object[] result : bestSellingProducts) {
            ProductDetails product = (ProductDetails) result[0];
            Long totalSold = (Long) result[1];

            bestSellersList.add(Map.of(
                    "name", product.getProducts().getProductName(),
                    "totalSold", totalSold,
                    "price", product.getPrice(),
                    "description", product.getProducts().getDescription()
            ));
        }
        return bestSellersList;
    }

    // 🔎 Lấy thông tin chi tiết sản phẩm theo tên sản phẩm trong câu hỏi
    private Map<String, Object> getProductDetails(String userMessage) {
        List<ProductsDTO> allProducts = productsService.getAllProductss();
        String userMessageLower = userMessage.toLowerCase();

        for (ProductsDTO product : allProducts) {
            String productNameLower = product.getProductName().toLowerCase();

            if (userMessageLower.contains(productNameLower)) {
                return Map.of(
                        "name", product.getProductName(),
                        "price", product.getPrice(),
                        "description", product.getDescription()
                );
            }
        }

        return Map.of("error", "Không tìm thấy sản phẩm bạn yêu cầu.");
    }

    // 🏪 Lấy thông tin cửa hàng
    private Map<String, Object> getStoreInformation() {
        return Map.of(
                "storeName", "PetCare Shop",
                "openingHours", "8:00 AM - 21:00 PM",
                "returnPolicy", "Đổi trả trong 7 ngày nếu có lỗi từ nhà sản xuất.",
                "contact", Map.of(
                        "phone", "0862287480",
                        "contactPerson", "Phát",
                        "email", "vothanhphat7480@gmail.com"
                )
        );
    }

    // 🚀 Gửi yêu cầu đến API Gemini
    private String sendRequestToAI(String userMessage, Map<String, Object> data) {
        try {
            String productsJson = objectMapper.writeValueAsString(data);
            String prompt = "Bạn là nhân viên tư vấn của PetCare. Đây là thông tin cửa hàng:\n\n"
                    + productsJson + "\n\nNgười dùng hỏi: " + userMessage
                    + "\n Hãy trả lời chi tiết, rõ ràng và dễ đọc."
                    + " Nếu người dùng hỏi về liên hệ, hãy cung cấp số điện thoại và email."
                    + " Nếu hỏi về giờ mở cửa, hãy cung cấp thông tin giờ hoạt động.";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("contents", new Object[]{
                    Map.of("parts", new Object[]{Map.of("text", prompt)})
            });

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "{ \"error\": \"Lỗi khi gửi yêu cầu đến AI!\" }";
        }
    }

    // 🛠️ Xử lý phản hồi từ AI
    private String formatAiResponse(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                return "{ \"error\": \"Không có phản hồi từ AI.\" }";
            }

            JsonNode contentParts = candidates.get(0).path("content").path("parts");
            if (!contentParts.isArray() || contentParts.isEmpty()) {
                return "{ \"error\": \"Không tìm thấy nội dung phản hồi.\" }";
            }

            return objectMapper.writeValueAsString(Map.of(
                    "status", "success",
                    "message", "Phản hồi từ AI",
                    "response", contentParts.get(0).path("text").asText(),
                    "timestamp", new Date().toString()
            ));
        } catch (Exception e) {
            return "{ \"error\": \"Lỗi khi xử lý phản hồi từ AI!\" }";
        }
    }
}
