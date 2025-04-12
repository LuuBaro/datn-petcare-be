package org.example.petcarebe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.example.petcarebe.dto.CartDetailsDTO;
import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.repository.OrderDetailsRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";

    private static final String GUIDELINE_API_URL =
            "https://apiasm2-112cb-default-rtdb.asia-southeast1.firebasedatabase.app/ai_guidelines.json";

    @Autowired
    private ProductsService productsService;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private CartDetailsService cartDetailsService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String getAiResponse(String userMessage, String defaultUserName, Long userId) {
        try {
            String userName = userRepository.findFullNameByUserId(userId);
            if (userName == null) {
                userName = defaultUserName;
                log.warn("Không tìm thấy userName cho userId: {}, dùng default: {}", userId, defaultUserName);
            }

            AiIntent intent = detectIntent(userMessage);
            Map<String, Object> context = buildContext(intent, userMessage, userId);
            String prompt = buildPrompt(userMessage, userName, context);
            String aiRawResponse = callGeminiApi(prompt);
            return extractAiResponse(aiRawResponse);
        } catch (Exception e) {
            log.error("Lỗi xử lý AI cho userId {}: {}", userId, e.getMessage());
            return jsonError("Đã xảy ra lỗi khi xử lý yêu cầu của bạn.");
        }
    }

    private AiIntent detectIntent(String message) {
        String lower = message.toLowerCase();
        if (containsKeyword(lower, "bán chạy", "phổ biến")) return AiIntent.BEST_SELLERS;
        if (containsKeyword(lower, "tất cả sản phẩm", "danh sách")) return AiIntent.ALL_PRODUCTS;
        if (containsKeyword(lower, "giá", "chi tiết", "thông tin sản phẩm")) return AiIntent.PRODUCT_DETAIL;
        if (containsKeyword(lower, "giỏ hàng", "cart", "xem giỏ")) return AiIntent.CART;
        if (containsKeyword(lower, "giờ mở cửa", "đổi trả", "liên hệ", "số điện thoại", "email")) return AiIntent.STORE_INFO;
        return AiIntent.DEFAULT;
    }

    private boolean containsKeyword(String message, String... keywords) {
        return Arrays.stream(keywords).anyMatch(message::contains);
    }

    private Map<String, Object> buildContext(AiIntent intent, String userMessage, Long userId) {
        Map<String, Object> context = new HashMap<>();
        switch (intent) {
            case BEST_SELLERS -> context.put("bestSellingProducts", getBestSellingProducts());
            case ALL_PRODUCTS, DEFAULT -> context.put("allProducts", getLimitedProductCards(10));
            case PRODUCT_DETAIL -> context.put("productDetails", findProductDetailHtml(userMessage));
            case CART -> context.put("cartItems", getCartItemsHtml(userId));
            case STORE_INFO -> context.put("storeInfo", getStoreInfo());
        }
        return context;
    }

    private String buildPrompt(String userMessage, String userName, Map<String, Object> context) throws Exception {
        String jsonContext = objectMapper.writeValueAsString(context);
        JsonNode guidelines = fetchAiGuidelinesFromFirebase();

        StringBuilder doPart = new StringBuilder();
        guidelines.path("do").forEach(rule -> doPart.append("- ").append(rule.asText()).append("\n"));

        StringBuilder dontPart = new StringBuilder();
        guidelines.path("dont").forEach(rule -> dontPart.append("- ").append(rule.asText()).append("\n"));

        StringBuilder goodExample = new StringBuilder();
        guidelines.path("examples").path("good").forEach(example -> {
            goodExample.append("Hỏi: ").append(example.path("question").asText()).append("\n")
                    .append("Trả lời: ").append(example.path("answer").asText()).append("\n\n");
        });

        StringBuilder badExample = new StringBuilder();
        guidelines.path("examples").path("bad").forEach(example -> {
            badExample.append("Hỏi: ").append(example.path("question").asText()).append("\n")
                    .append("Trả lời: ").append(example.path("answer").asText()).append("\n\n");
        });

        return """
            Bạn là trợ lý AI của cửa hàng PetCare – chuyên cung cấp sản phẩm và dịch vụ cho thú cưng.
            Tên khách hàng hiện tại: %s

            🎯 Nhiệm vụ:
            Trả lời chính xác, lịch sự, theo quy tắc sau:

            ✅ Nên làm:
            %s

            ❌ Không nên:
            %s

            🔍 Ví dụ tốt:
            %s

            ⚠️ Ví dụ không nên:
            %s

            -----------------------------
            Dữ liệu hiện có (JSON hoặc HTML): 
            %s

            Câu hỏi người dùng: "%s"
            """.formatted(
                userName,
                doPart.toString(),
                dontPart.toString(),
                goodExample.toString(),
                badExample.toString(),
                jsonContext,
                userMessage
        );
    }

    private JsonNode fetchAiGuidelinesFromFirebase() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(GUIDELINE_API_URL, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Không thể tải guidelines từ Firebase:", e);
            return objectMapper.createObjectNode();
        }
    }

    private String callGeminiApi(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("contents", new Object[]{
                    Map.of("parts", new Object[]{Map.of("text", prompt)})
            });

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = String.format(GEMINI_API_URL, geminiApiKey);

            return restTemplate.postForEntity(url, entity, String.class).getBody();
        } catch (HttpClientErrorException e) {
            log.error("Gemini API lỗi: {}", e.getResponseBodyAsString());
            return jsonError("Lỗi từ AI: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Gọi Gemini API thất bại:", e);
            return jsonError("Không thể kết nối đến AI.");
        }
    }

    private String extractAiResponse(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) return jsonError("Phản hồi AI rỗng.");
            return objectMapper.writeValueAsString(Map.of("status", "success", "response", parts.get(0).path("text").asText()));
        } catch (Exception e) {
            log.error("Lỗi phân tích JSON AI:", e);
            return jsonError("Không thể hiểu phản hồi từ AI.");
        }
    }

    private List<String> getBestSellingProducts() {
        Pageable top3 = PageRequest.of(0, 3);
        List<Object[]> top = orderDetailsRepository.findBestSellingProducts(top3);
        List<String> result = new ArrayList<>();
        for (Object[] row : top) {
            ProductDetails detail = (ProductDetails) row[0];
            result.add(toProductCard(detail.getProducts().getImage(), detail.getProducts().getProductName(), detail.getPrice()));
        }
        return result;
    }

    private String getLimitedProductCards(int limit) {
        List<ProductsDTO> products = productsService.getAllProductss().stream().limit(limit).toList();
        StringBuilder html = new StringBuilder("<div class='flex flex-col gap-4'>");
        for (ProductsDTO product : products) {
            html.append(toProductCard(product.getImage(), product.getProductName(), product.getPrice()));
        }
        html.append("</div>");
        return html.toString();
    }

    private String findProductDetailHtml(String userMessage) {
        String lower = userMessage.toLowerCase(); // Sửa lỗi: dùng userMessage thay vì message
        return productsService.getAllProductss().stream()
                .filter(p -> lower.contains(p.getProductName().toLowerCase()))
                .findFirst()
                .map(p -> toProductCard(p.getImage(), p.getProductName(), p.getPrice()))
                .orElse("<p class='text-red-500'>Không tìm thấy sản phẩm bạn yêu cầu.</p>");
    }

    private String getCartItemsHtml(Long userId) {
        List<CartDetailsDTO> cartItems = cartDetailsService.getCartDetailsByUserId(userId);
        if (cartItems.isEmpty()) {
            return "<p class='text-gray-500'>Giỏ hàng của bạn hiện tại trống.</p>";
        }

        StringBuilder html = new StringBuilder("<div class='flex flex-col gap-4'>");
        for (CartDetailsDTO item : cartItems) {
            html.append(toCartItemCard(item));
        }
        html.append("</div>");
        return html.toString();
    }

    private String toCartItemCard(CartDetailsDTO item) {
        return String.format("""
            <div class='bg-white shadow-md rounded-lg flex w-[480px] h-[120px] overflow-hidden'>
                <img src='%s' alt='%s' class='w-[120px] h-full object-cover'>
                <div class='p-4 flex flex-col justify-center'>
                    <h3 class='text-base font-semibold text-gray-800 mb-1'>%s</h3>
                    <p class='text-gray-600 text-sm'>Giá: %,.0f VND</p>
                    <p class='text-gray-600 text-sm'>Số lượng: %d</p>
                </div>
            </div>
            """, item.getImage(), item.getProductName(), item.getProductName(), item.getPrice(), item.getQuantityItem());
    }

    @Cacheable("storeInfo")
    private Map<String, Object> getStoreInfo() {
        return Map.of(
                "storeName", "PetCare Shop",
                "openingHours", "8:00 - 21:00",
                "returnPolicy", "Đổi trả trong 7 ngày nếu có lỗi sản phẩm.",
                "contact", Map.of(
                        "phone", "0862287480",
                        "email", "vothanhphat7480@gmail.com",
                        "person", "Phát"
                )
        );
    }

    private String toProductCard(String img, String name, double price) {
        return String.format("""
            <div class='bg-white shadow-md rounded-lg flex w-[480px] h-[120px] overflow-hidden'>
                <img src='%s' alt='%s' class='w-[120px] h-full object-cover'>
                <div class='p-4 flex flex-col justify-center'>
                    <h3 class='text-base font-semibold text-gray-800 mb-1'>%s</h3>
                    <p class='text-gray-600 text-sm'>Giá: %,.0f VND</p>
                </div>
            </div>
            """, img, name, name, price);
    }

    private String jsonError(String message) {
        return String.format("{ \"status\": \"error\", \"message\": \"%s\" }", message);
    }

    enum AiIntent {
        BEST_SELLERS,
        ALL_PRODUCTS,
        PRODUCT_DETAIL,
        CART,
        STORE_INFO,
        DEFAULT
    }
}