package org.example.petcarebe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.petcarebe.dto.ProductDetailsDTO;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private ProductDetailsService productDetailsService;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<Long, ConversationContext> userContexts = new ConcurrentHashMap<>();

    private static final Map<String, String> CATEGORY_SYNONYMS = new HashMap<>();

    private static final Set<String> SUPPORTED_PETS = new HashSet<>(Arrays.asList("chó", "mèo"));

    private static final Set<String> UNSUPPORTED_PETS = new HashSet<>(Arrays.asList("cá", "bò", "người"));

    static {
        // Chuồng
        CATEGORY_SYNONYMS.put("đệm", "chuồng");
        CATEGORY_SYNONYMS.put("nệm", "chuồng");
        CATEGORY_SYNONYMS.put("giường", "chuồng");
        CATEGORY_SYNONYMS.put("nhà", "chuồng");
        CATEGORY_SYNONYMS.put("ổ", "chuồng");
        CATEGORY_SYNONYMS.put("bed", "chuồng");
        CATEGORY_SYNONYMS.put("lồng", "chuồng");
        CATEGORY_SYNONYMS.put("chuồng", "chuồng");
        CATEGORY_SYNONYMS.put("kennel", "chuồng");

        // Thức ăn
        CATEGORY_SYNONYMS.put("thức ăn", "thức ăn");
        CATEGORY_SYNONYMS.put("đồ ăn", "thức ăn");
        CATEGORY_SYNONYMS.put("bánh thưởng", "thức ăn");
        CATEGORY_SYNONYMS.put("funny", "thức ăn");
        CATEGORY_SYNONYMS.put("food", "thức ăn");
        CATEGORY_SYNONYMS.put("hạt", "thức ăn");
        CATEGORY_SYNONYMS.put("pate", "thức ăn");
        CATEGORY_SYNONYMS.put("pâté", "thức ăn");
        CATEGORY_SYNONYMS.put("snack", "thức ăn");
        CATEGORY_SYNONYMS.put("treat", "thức ăn");
        CATEGORY_SYNONYMS.put("sữa", "thức ăn");

        // Đồ chơi
        CATEGORY_SYNONYMS.put("đồ chơi", "đồ chơi");
        CATEGORY_SYNONYMS.put("trò chơi", "đồ chơi");
        CATEGORY_SYNONYMS.put("đồ vui chơi", "đồ chơi");
        CATEGORY_SYNONYMS.put("toy", "đồ chơi");
        CATEGORY_SYNONYMS.put("bóng", "đồ chơi");
        CATEGORY_SYNONYMS.put("cần câu", "đồ chơi");
        CATEGORY_SYNONYMS.put("cào móng", "đồ chơi");

        // Phụ kiện
        CATEGORY_SYNONYMS.put("sữa tắm", "phụ kiện");
        CATEGORY_SYNONYMS.put("shampoo", "phụ kiện");
        CATEGORY_SYNONYMS.put("dầu gội", "phụ kiện");
        CATEGORY_SYNONYMS.put("xà bông", "phụ kiện");
        CATEGORY_SYNONYMS.put("xịt khử mùi", "phụ kiện");
        CATEGORY_SYNONYMS.put("vòng cổ", "phụ kiện");
        CATEGORY_SYNONYMS.put("collar", "phụ kiện");
        CATEGORY_SYNONYMS.put("dây dắt", "phụ kiện");
        CATEGORY_SYNONYMS.put("leash", "phụ kiện");
        CATEGORY_SYNONYMS.put("yếm", "phụ kiện");
        CATEGORY_SYNONYMS.put("quần áo", "phụ kiện");
        CATEGORY_SYNONYMS.put("áo", "phụ kiện");
        CATEGORY_SYNONYMS.put("phụ kiện", "phụ kiện");

        // Kết hợp với thú cưng cụ thể
        CATEGORY_SYNONYMS.put("đệm mèo", "chuồng");
        CATEGORY_SYNONYMS.put("đệm chó", "chuồng");
        CATEGORY_SYNONYMS.put("thức ăn mèo", "thức ăn");
        CATEGORY_SYNONYMS.put("thức ăn chó", "thức ăn");
        CATEGORY_SYNONYMS.put("đồ chơi mèo", "đồ chơi");
        CATEGORY_SYNONYMS.put("đồ chơi chó", "đồ chơi");
        CATEGORY_SYNONYMS.put("sữa tắm mèo", "phụ kiện");
        CATEGORY_SYNONYMS.put("sữa tắm chó", "phụ kiện");
    }

    // Hàm xử lý yêu cầu từ người dùng và trả về phản hồi từ AI
    public String getAiResponse(String userMessage, String defaultUserName, Long userId) {
        try {
            // Lấy tên người dùng từ database, nếu không có thì dùng tên mặc định
            String userName = userRepository.findFullNameByUserId(userId);
            if (userName == null) {
                userName = defaultUserName;
                log.warn("Không tìm thấy tên người dùng cho userId: {}, sử dụng mặc định: {}", userId, defaultUserName);
            }

            // Lấy hoặc tạo ngữ cảnh cuộc hội thoại cho người dùng
            ConversationContext context = userContexts.computeIfAbsent(userId, k -> new ConversationContext());
            context.addMessage(userMessage);
            context.setUserId(userId);

            // Xác định ý định của người dùng (intent)
            AiIntent intent = detectIntent(userMessage, context);
            // Tạo ngữ cảnh dữ liệu dựa trên ý định
            Map<String, Object> dataContext = buildContext(intent, userMessage, userId, context);
            // Tạo prompt gửi đến Gemini API
            String prompt = buildPrompt(userMessage, userName, dataContext);
            // Gọi Gemini API để lấy phản hồi
            String aiRawResponse = callGeminiApi(prompt);
            // Xử lý phản hồi từ AI
            String response = extractAndProcessResponse(aiRawResponse, dataContext, intent, context, userMessage);

            context.setLastIntent(intent);
            return response;
        } catch (Exception e) {
            log.error("Lỗi xử lý AI cho userId {}: {}", userId, e.getMessage());
            return jsonError("Đã có lỗi xảy ra khi xử lý yêu cầu của anh/chị.");
        }
    }

    // Hàm xác định ý định (intent) của người dùng dựa trên tin nhắn
    private AiIntent detectIntent(String message, ConversationContext context) {
        String lower = message.toLowerCase().trim();

        // Kiểm tra các yêu cầu không hợp lệ (thú cưng không hỗ trợ hoặc cho người)
        boolean isUnsupportedPet = UNSUPPORTED_PETS.stream().anyMatch(pet -> lower.contains(pet));
        if (isUnsupportedPet || containsKeyword(lower, "cho người", "người dùng")) {
            return AiIntent.UNSUPPORTED_PET;
        }

        if (containsKeyword(lower, "bán chạy", "phổ biến")) return AiIntent.BEST_SELLERS;
        if (containsKeyword(lower, "tất cả sản phẩm", "danh sách sản phẩm")) return AiIntent.ALL_PRODUCTS;
        if (containsKeyword(lower, "giá thấp nhất", "rẻ nhất", "giá rẻ")) {
            if (containsKeyword(lower, "thức ăn", "bánh thưởng", "mèo", "chó", "sữa tắm")) {
                context.setLastCategory(containsKeyword(lower, "sữa tắm") ? "phụ kiện" : "thức ăn");
                return AiIntent.PRICE_FILTER_MIN;
            }
            return AiIntent.PRICE_FILTER_MIN;
        }
        if (containsKeyword(lower, "giá cao nhất", "đắt nhất")) return AiIntent.PRICE_FILTER_MAX;
        if (containsKeyword(lower, "giá", "chi tiết", "thông tin sản phẩm", "xem sản phẩm", "bánh thưởng", "funny", "sữa tắm", "camellia")) {
            return AiIntent.PRODUCT_DETAIL;
        }
        if (containsKeyword(lower, "giờ mở cửa", "đổi trả", "liên hệ", "số điện thoại", "email", "thông tin của cửa hàng")) return AiIntent.STORE_INFO;
        if (containsKeyword(lower, "xem thêm", "nhiều hơn", "tiếp theo", "sản phẩm khác")) {
            if (context.getLastIntent() == AiIntent.CATEGORY_FILTER ||
                    context.getLastIntent() == AiIntent.PRICE_FILTER_MIN ||
                    context.getLastIntent() == AiIntent.PRICE_FILTER_MAX ||
                    context.getLastIntent() == AiIntent.ALL_PRODUCTS) {
                return AiIntent.VIEW_MORE;
            }
        }
        if (containsKeyword(lower, "liên quan", "tương tự", "cùng loại")) {
            if (context.getLastCategory() != null) {
                return AiIntent.CATEGORY_FILTER;
            }
        }

        // Nếu yêu cầu sản phẩm không hợp lệ, chuyển sang CATEGORY_FILTER
        boolean hasCategory = false;
        for (Map.Entry<String, String> entry : CATEGORY_SYNONYMS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                context.setLastCategory(entry.getValue());
                hasCategory = true;
                break;
            }
        }

        // Nếu có từ khóa liên quan đến mèo hoặc chó, tiếp tục xử lý
        if (!hasCategory && SUPPORTED_PETS.stream().anyMatch(pet -> lower.contains(pet))) {
            if (lower.contains("điện thoại") || lower.contains("máy tính") || lower.contains("xe máy")) {
                context.setLastCategory("đồ chơi"); // Gợi ý đồ chơi nếu sản phẩm không hợp lệ
                return AiIntent.CATEGORY_FILTER;
            }
            context.setLastCategory("thức ăn"); // Mặc định gợi ý thức ăn
            return AiIntent.CATEGORY_FILTER;
        }

        if (hasCategory) {
            return AiIntent.CATEGORY_FILTER;
        }

        if (context.getLastIntent() != null && !lower.contains("khác")) {
            if (context.getLastIntent() == AiIntent.CATEGORY_FILTER && containsKeyword(lower, "giá", "rẻ")) {
                return AiIntent.PRICE_FILTER_MIN;
            }
            if (context.getLastIntent() == AiIntent.CATEGORY_FILTER && containsKeyword(lower, "đắt")) {
                return AiIntent.PRICE_FILTER_MAX;
            }
            // Kiểm tra nếu người dùng hỏi lại về sản phẩm vừa tìm (ví dụ: "sữa tắm này cho bò tắm được không")
            if (context.getLastIntent() == AiIntent.PRODUCT_DETAIL || context.getLastIntent() == AiIntent.CATEGORY_FILTER) {
                if (UNSUPPORTED_PETS.stream().anyMatch(pet -> lower.contains(pet)) || containsKeyword(lower, "cho người", "người dùng")) {
                    return AiIntent.UNSUPPORTED_PET;
                }
            }
        }

        for (String prevMessage : context.getMessageHistory()) {
            for (Map.Entry<String, String> entry : CATEGORY_SYNONYMS.entrySet()) {
                if (prevMessage.toLowerCase().contains(entry.getKey()) && context.getLastCategory() == null) {
                    context.setLastCategory(entry.getValue());
                    return AiIntent.CATEGORY_FILTER;
                }
            }
        }

        return AiIntent.DEFAULT;
    }

    // Hàm kiểm tra xem tin nhắn có chứa từ khóa nào không
    private boolean containsKeyword(String message, String... keywords) {
        return Arrays.stream(keywords).anyMatch(keyword -> message.contains(keyword.toLowerCase()));
    }

    // Hàm tạo ngữ cảnh dữ liệu dựa trên ý định của người dùng
    private Map<String, Object> buildContext(AiIntent intent, String userMessage, Long userId, ConversationContext context) {
        Map<String, Object> dataContext = new HashMap<>();
        switch (intent) {
            case BEST_SELLERS -> dataContext.put("bestSellingProducts", getBestSellingProducts());
            case ALL_PRODUCTS -> {
                dataContext.put("allProducts", getLimitedProductCards(10, userMessage, context, 0));
                context.resetPagination();
            }
            case CATEGORY_FILTER -> {
                dataContext.put("allProducts", getLimitedProductCards(10, userMessage, context, 0));
                context.resetPagination();
            }
            case PRICE_FILTER_MIN -> {
                dataContext.put("allProducts", getLimitedProductCards(5, userMessage, context, 0));
                context.resetPagination();
            }
            case PRICE_FILTER_MAX -> {
                dataContext.put("allProducts", getLimitedProductCards(5, userMessage, context, 0));
                context.resetPagination();
            }
            case VIEW_MORE -> {
                int offset = context.getPageOffset(context.getLastCategory() != null ? context.getLastCategory() : "all");
                dataContext.put("allProducts", getLimitedProductCards(10, context.getLastCategory() != null ? context.getLastCategory() : userMessage, context, offset));
            }
            case PRODUCT_DETAIL -> dataContext.put("productDetails", findProductDetailHtml(userMessage));
            case STORE_INFO -> dataContext.put("storeInfo", getStoreInfo());
            case UNSUPPORTED_PET -> dataContext.put("unsupportedPetMessage", "<p className='text-gray-500 mb-4'>Dạ, hiện tại PetCare chỉ có sản phẩm cho chó và mèo thôi ạ. Anh/chị có muốn em gợi ý gì cho bé cưng nhà mình không?</p>");
            case DEFAULT -> dataContext.put("defaultMessage", "<p className='text-gray-500 mb-4'>Anh/chị ơi, em chưa hiểu rõ ý lắm. Anh/chị muốn tìm thức ăn, đồ chơi, phụ kiện hay sữa tắm cho bé cưng nhà mình ạ?</p>");
        }
        return dataContext;
    }

    // Hàm tạo prompt gửi đến Gemini API
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
            Tên khách hàng: %s

            🎯 Nhiệm vụ:
            - Trả lời chính xác, tự nhiên, thân thiện như một người bạn.
            - Cửa hàng PetCare chỉ bán sản phẩm cho CHÓ và MÈO. Nếu người dùng hỏi về các loại thú cưng khác (như cá, bò, v.v.) hoặc yêu cầu không hợp lý (như "cho người"), trả lời lịch sự: "Dạ, hiện tại PetCare chỉ có sản phẩm cho chó và mèo thôi ạ. Anh/chị có muốn em gợi ý gì cho bé cưng nhà mình không?".
            - Khi trả lời về sản phẩm (tất cả sản phẩm, danh mục, giá, chi tiết, xem thêm), PHẢI trả về HTML hoàn chỉnh với các thẻ product-card trong product-list, KHÔNG trả về JSON, Markdown, placeholder như {{products}} hay ${allProducts}, hoặc văn bản thô.
            - Nếu dữ liệu trong context đã là HTML hoàn chỉnh (ví dụ: bestSellingProducts), bạn PHẢI sử dụng trực tiếp dữ liệu đó và KHÔNG thay đổi định dạng, KHÔNG tạo lại dữ liệu thô hoặc văn bản khác.
            - Khi trả lời về thông tin cửa hàng (storeInfo), trả về văn bản dạng HTML với các thẻ <p>, hiển thị đầy đủ thông tin: tên cửa hàng, giờ mở cửa, chính sách đổi trả, và thông tin liên hệ (số điện thoại, email, người liên hệ).
            - Khi người dùng hỏi sản phẩm có phù hợp với đối tượng không hợp lệ (ví dụ: "sữa tắm này cho bò tắm được không"), trả lời lịch sự: "Dạ, sản phẩm này chỉ phù hợp cho chó và mèo thôi ạ. Anh/chị có muốn em tìm sản phẩm khác cho bé cưng nhà mình không?".
            - KHÔNG tự tạo dữ liệu giả hoặc sản phẩm không tồn tại. Chỉ sử dụng dữ liệu từ context (đã được lấy từ cơ sở dữ liệu).
            - Sử dụng template 'product_card' với các trường: image, name, short description, price, và badge 'Sắp hết hàng' nếu quantity < 5.
            - Dùng className thay vì class để tương thích với React.
            - Đảm bảo product-list hiển thị dạng grid (grid-cols-1 sm:grid-cols-2 lg:grid-cols-3) với khoảng cách hợp lý.
            - Nếu yêu cầu sản phẩm cụ thể (ví dụ: "Sữa tắm hoa trà CAMELLIA"), tìm kiếm chính xác trong cơ sở dữ liệu, kể cả khi người dùng nhập ngắn gọn (như "sữa tắm").
            - Nếu yêu cầu "giá rẻ", hiển thị tối đa 5 sản phẩm có giá thấp nhất trong danh mục liên quan, loại bỏ trùng lặp.
            - Nếu yêu cầu "xem thêm" hoặc "sản phẩm liên quan", tiếp tục hiển thị sản phẩm từ danh mục trước đó.
            - Nếu không tìm thấy sản phẩm, gợi ý danh mục tương tự trong HTML (ví dụ: thức ăn, đồ chơi, phụ kiện) nhưng KHÔNG tự tạo sản phẩm giả.
            - Duy trì ngữ cảnh từ các câu hỏi trước để trả lời mạch lạc.
            - Tránh báo sai rằng sản phẩm không tồn tại khi nó có trong cơ sở dữ liệu.

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

    // Hàm lấy hướng dẫn từ Firebase để định dạng phản hồi của AI
    private JsonNode fetchAiGuidelinesFromFirebase() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(GUIDELINE_API_URL, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Lỗi lấy hướng dẫn từ Firebase:", e);
            return objectMapper.createObjectNode();
        }
    }

    // Hàm gọi Gemini API để lấy phản hồi dựa trên prompt
    private String callGeminiApi(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{Map.of(
                            "parts", new Object[]{Map.of("text", prompt)}
                    )}
            );

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = String.format(GEMINI_API_URL, geminiApiKey);

            String response = restTemplate.postForEntity(url, entity, String.class).getBody();
            log.info("Phản hồi thô từ Gemini: {}", response);
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Lỗi API Gemini: {}", e.getResponseBodyAsString());
            return jsonError("Lỗi từ AI: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Lỗi gọi API Gemini:", e);
            return jsonError("Không thể kết nối đến AI.");
        }
    }

    // Hàm xử lý phản hồi từ Gemini API và trả về kết quả phù hợp
    private String extractAndProcessResponse(String aiResponse, Map<String, Object> context, AiIntent intent, ConversationContext conversationContext, String userMessage) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return jsonError("Phản hồi AI rỗng.");
            }

            String text = parts.get(0).path("text").asText();

            // Loại bỏ Markdown nếu có
            text = text.replaceAll("```html\\n?|```\\n?", "");

            // Kiểm tra xem phản hồi có phải là HTML hợp lệ không
            boolean isHtml = text.trim().startsWith("<") && text.trim().endsWith(">");

            // Xử lý ý định UNSUPPORTED_PET
            if (intent == AiIntent.UNSUPPORTED_PET && context.containsKey("unsupportedPetMessage")) {
                return (String) context.get("unsupportedPetMessage");
            }

            // Xử lý ý định STORE_INFO
            if (intent == AiIntent.STORE_INFO && context.containsKey("storeInfo")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> storeInfo = (Map<String, Object>) context.get("storeInfo");
                return formatStoreInfoToHtml(storeInfo);
            }

            // Nếu phản hồi không phải HTML hoặc có chứa placeholder không hợp lệ, sử dụng dữ liệu từ context
            if (!isHtml || text.contains("Placeholder") || text.contains("Product Image")) {
                StringBuilder htmlResponse = new StringBuilder();
                if (intent == AiIntent.BEST_SELLERS && context.containsKey("bestSellingProducts")) {
                    @SuppressWarnings("unchecked")
                    List<String> bestSellingProducts = (List<String>) context.get("bestSellingProducts");
                    if (!bestSellingProducts.isEmpty()) {
                        htmlResponse.append("<div className='product-list grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 p-6'>");
                        bestSellingProducts.forEach(htmlResponse::append);
                        htmlResponse.append("</div>");
                    } else {
                        htmlResponse.append("<p className='text-gray-500 mb-4'>Hiện tại chưa có sản phẩm bán chạy nào.</p>");
                    }
                } else if (intent == AiIntent.ALL_PRODUCTS || intent == AiIntent.CATEGORY_FILTER ||
                        intent == AiIntent.PRICE_FILTER_MIN || intent == AiIntent.PRICE_FILTER_MAX ||
                        intent == AiIntent.VIEW_MORE) {
                    if (context.containsKey("allProducts")) {
                        htmlResponse.append((String) context.get("allProducts"));
                    }
                } else if (intent == AiIntent.PRODUCT_DETAIL && context.containsKey("productDetails")) {
                    htmlResponse.append((String) context.get("productDetails"));
                } else if (intent == AiIntent.DEFAULT && context.containsKey("defaultMessage")) {
                    htmlResponse.append((String) context.get("defaultMessage"));
                } else {
                    htmlResponse.append("<p className='text-gray-500 mb-4'>Anh/chị ơi, em chưa hiểu rõ ý lắm. Anh/chị muốn tìm thức ăn, đồ chơi, phụ kiện hay sữa tắm cho bé cưng nhà mình ạ?</p>");
                }
                text = htmlResponse.toString();
            }

            // Xử lý phân trang cho VIEW_MORE
            if (intent == AiIntent.VIEW_MORE) {
                String category = conversationContext.getLastCategory() != null ? conversationContext.getLastCategory() : "all";
                conversationContext.incrementPageOffset(category, 10);
            }

            // Trả về HTML thuần túy cho các intent sản phẩm
            if (intent == AiIntent.ALL_PRODUCTS ||
                    intent == AiIntent.CATEGORY_FILTER ||
                    intent == AiIntent.PRICE_FILTER_MIN ||
                    intent == AiIntent.PRICE_FILTER_MAX ||
                    intent == AiIntent.VIEW_MORE ||
                    intent == AiIntent.PRODUCT_DETAIL ||
                    intent == AiIntent.BEST_SELLERS ||
                    intent == AiIntent.STORE_INFO ||
                    intent == AiIntent.UNSUPPORTED_PET) {
                return text;
            }

            // Trả về JSON cho các intent khác
            return objectMapper.writeValueAsString(Map.of("status", "success", "response", text));
        } catch (Exception e) {
            log.error("Lỗi phân tích phản hồi AI:", e);
            return jsonError("Không thể hiểu phản hồi từ AI.");
        }
    }

    // Hàm định dạng thông tin cửa hàng thành HTML
    private String formatStoreInfoToHtml(Map<String, Object> storeInfo) {
        @SuppressWarnings("unchecked")
        Map<String, String> contact = (Map<String, String>) storeInfo.get("contact");
        return String.format("""
            <div className='store-info p-6 bg-white shadow-md rounded-lg'>
                <h3 className='text-lg font-semibold text-gray-800 mb-4'>Thông tin cửa hàng %s</h3>
                <p className='text-gray-600 mb-2'><strong>Giờ mở cửa:</strong> %s</p>
                <p className='text-gray-600 mb-2'><strong>Chính sách đổi trả:</strong> %s</p>
                <p className='text-gray-600 mb-2'><strong>Liên hệ:</strong></p>
                <ul className='list-disc list-inside text-gray-600'>
                    <li>Số điện thoại: %s</li>
                    <li>Email: %s</li>
                    <li>Người liên hệ: %s</li>
                </ul>
            </div>
            """,
                storeInfo.get("storeName"),
                storeInfo.get("openingHours"),
                storeInfo.get("returnPolicy"),
                contact.get("phone"),
                contact.get("email"),
                contact.get("person")
        );
    }

    // Hàm lấy danh sách sản phẩm bán chạy nhất (top 3)
    private List<String> getBestSellingProducts() {
        Pageable top3 = PageRequest.of(0, 3);
        List<Object[]> top = orderDetailsRepository.findBestSellingProducts(top3);
        List<String> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (Object[] row : top) {
            ProductDetails detail = (ProductDetails) row[0];
            if (seenIds.contains(detail.getProductDetailId())) {
                log.warn("Phát hiện sản phẩm trùng lặp trong bán chạy: ID {}", detail.getProductDetailId());
                continue;
            }
            try {
                ProductDetailsDTO dto = productDetailsService.getProductDetailsById(detail.getProductDetailId());
                log.info("BestSelling Product: Name={}, ProductDetailId={}", dto.getProductName(), dto.getProductDetailId());
                result.add(toProductCard(
                        dto.getProductImage(),
                        dto.getProductName(),
                        dto.getPrice(),
                        dto.getDescription(),
                        dto.getCategoryName(),
                        dto.getColorValue(),
                        dto.getSizeValue(),
                        dto.getWeightValue(),
                        dto.getQuantity(),
                        dto.getProductDetailId()
                ));
                seenIds.add(dto.getProductDetailId());
            } catch (RuntimeException e) {
                log.warn("Không tìm thấy chi tiết sản phẩm cho ID {}: {}", detail.getProductDetailId(), e.getMessage());
            }
        }
        if (result.isEmpty()) {
            result.add("<p className='text-gray-500 mb-4'>Hiện tại chưa có sản phẩm bán chạy nào.</p>");
        }
        return result;
    }

    // Hàm lấy danh sách sản phẩm giới hạn (theo số lượng, danh mục, giá, v.v.)
    private String getLimitedProductCards(int limit, String userMessage, ConversationContext context, int offset) {
        List<ProductDetailsDTO> products;
        try {
            products = productDetailsService.findAllProductDetails();
            log.info("Lấy được {} sản phẩm từ cơ sở dữ liệu", products.size());
        } catch (RuntimeException e) {
            log.error("Lỗi lấy sản phẩm từ cơ sở dữ liệu: {}", e.getMessage());
            return "<p className='text-red-500 mb-4'>Hiện tại không thể hiển thị sản phẩm. Anh/chị vui lòng thử lại sau nhé!</p>";
        }

        if (products.isEmpty()) {
            log.warn("Danh sách sản phẩm rỗng từ cơ sở dữ liệu");
            return "<p className='text-gray-500 mb-4'>Hiện tại cửa hàng chưa có sản phẩm nào để hiển thị. Anh/chị muốn em gợi ý gì khác không ạ?</p>";
        }

        String lowerMessage = userMessage.toLowerCase();
        String filterCategory = context.getLastCategory();
        boolean sortByPriceMin = lowerMessage.contains("giá thấp nhất") || lowerMessage.contains("rẻ nhất") || lowerMessage.contains("giá rẻ");
        boolean sortByPriceMax = lowerMessage.contains("giá cao nhất") || lowerMessage.contains("đắt nhất");

        // Xác định danh mục từ khóa
        if (filterCategory == null) {
            for (Map.Entry<String, String> entry : CATEGORY_SYNONYMS.entrySet()) {
                if (lowerMessage.contains(entry.getKey())) {
                    filterCategory = entry.getValue();
                    context.setLastCategory(filterCategory);
                    break;
                }
            }
        }

        // Lọc sản phẩm theo danh mục và từ khóa bổ sung
        List<ProductDetailsDTO> filteredProducts = products;
        if (filterCategory != null && !filterCategory.equals("all")) {
            final String categoryFilter = filterCategory.toLowerCase();
            filteredProducts = products.stream()
                    .filter(p -> p.getCategoryName() != null &&
                            (p.getCategoryName().toLowerCase().contains(categoryFilter) ||
                                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(categoryFilter)) ||
                                    (p.getProductName() != null && p.getProductName().toLowerCase().contains(categoryFilter))))
                    .toList();
        }

        // Lọc thêm theo mèo hoặc chó
        if (lowerMessage.contains("mèo")) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> (p.getDescription() != null && p.getDescription().toLowerCase().contains("mèo")) ||
                            (p.getProductName() != null && p.getProductName().toLowerCase().contains("mèo")))
                    .toList();
        } else if (lowerMessage.contains("chó")) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> (p.getDescription() != null && p.getDescription().toLowerCase().contains("chó")) ||
                            (p.getProductName() != null && p.getProductName().toLowerCase().contains("chó")))
                    .toList();
        } else if (lowerMessage.contains("sữa tắm") || lowerMessage.contains("camellia")) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> (p.getProductName() != null && p.getProductName().toLowerCase().contains("sữa tắm")) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains("sữa tắm")) ||
                            (p.getProductName() != null && p.getProductName().toLowerCase().contains("camellia")))
                    .toList();
        }

        // Loại bỏ trùng lặp
        Map<String, ProductDetailsDTO> uniqueProducts = new HashMap<>();
        for (ProductDetailsDTO p : filteredProducts) {
            String key = p.getProductName() + "-" + p.getDescription();
            if (!uniqueProducts.containsKey(key) || p.getPrice() < uniqueProducts.get(key).getPrice()) {
                uniqueProducts.put(key, p);
            } else {
                log.warn("Phát hiện sản phẩm trùng lặp: {} với giá {}", p.getProductName(), p.getPrice());
            }
        }
        filteredProducts = new ArrayList<>(uniqueProducts.values());

        // Sắp xếp theo giá
        if (sortByPriceMin) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getQuantity() > 0)
                    .sorted(Comparator.comparingDouble(ProductDetailsDTO::getPrice))
                    .toList();
        } else if (sortByPriceMax) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getQuantity() > 0)
                    .sorted(Comparator.comparingDouble(ProductDetailsDTO::getPrice).reversed())
                    .toList();
        }

        // Giới hạn số lượng
        filteredProducts = filteredProducts.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        if (filteredProducts.isEmpty()) {
            log.info("Không tìm thấy sản phẩm phù hợp cho yêu cầu: {}", userMessage);
            String suggestion = filterCategory != null
                    ? String.format("<p className='text-gray-500 mb-4'>Hiện tại không có %s nào phù hợp. Anh/chị muốn xem thức ăn, đồ chơi, phụ kiện hay sữa tắm cho bé cưng không ạ?</p>", filterCategory)
                    : "<p className='text-gray-500 mb-4'>Hiện tại không tìm thấy sản phẩm phù hợp. Anh/chị muốn xem thức ăn, đồ chơi, phụ kiện hay sữa tắm không ạ?</p>";
            return suggestion;
        }

        context.getDisplayedProducts().clear();
        for (ProductDetailsDTO product : filteredProducts) {
            context.getDisplayedProducts().put(product.getProductName(), product.getProductDetailId());
        }

        StringBuilder html = new StringBuilder("<div className='product-list grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 p-6'>");
        for (ProductDetailsDTO product : filteredProducts) {
            if (product.getPrice() > 1_000_000) {
                log.warn("Sản phẩm {} có giá bất thường: {}", product.getProductName(), product.getPrice());
            }
            log.info("Limited Product: Name={}, ProductDetailId={}", product.getProductName(), product.getProductDetailId());
            String card = toProductCard(
                    product.getProductImage(),
                    product.getProductName(),
                    product.getPrice(),
                    product.getDescription(),
                    product.getCategoryName(),
                    product.getColorValue(),
                    product.getSizeValue(),
                    product.getWeightValue(),
                    product.getQuantity(),
                    product.getProductDetailId()
            );
            html.append(card);
        }
        html.append("</div>");
        return html.toString();
    }

    // Hàm tìm chi tiết sản phẩm dựa trên tin nhắn và trả về HTML
    private String findProductDetailHtml(String userMessage) {
        String lower = userMessage.toLowerCase();
        List<ProductDetailsDTO> products;
        try {
            products = productDetailsService.findAllProductDetails();
        } catch (RuntimeException e) {
            log.error("Lỗi lấy sản phẩm: {}", e.getMessage());
            return "<p className='text-red-500 mb-4'>Hiện tại không thể hiển thị sản phẩm. Anh/chị vui lòng thử lại sau nhé!</p>";
        }

        String productName = extractProductNameFromMessage(userMessage);
        // Tách từ khóa thành danh sách
        List<String> keywords = Arrays.stream(productName.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Tìm sản phẩm khớp với từ khóa
        Optional<ProductDetailsDTO> matchingProduct = products.stream()
                .filter(p -> p.getProductName() != null)
                .max(Comparator.comparingInt(p -> {
                    String nameLower = p.getProductName().toLowerCase();
                    int matches = (int) keywords.stream().filter(k -> nameLower.contains(k)).count();
                    if (nameLower.replaceAll("[^a-z0-9]", "").contains(productName.replaceAll("[^a-z0-9]", ""))) {
                        matches += keywords.size(); // Ưu tiên khớp toàn bộ tên
                    }
                    return matches;
                }));

        if (matchingProduct.isPresent()) {
            ProductDetailsDTO p = matchingProduct.get();
            log.info("Product Detail: Name={}, ProductDetailId={}", p.getProductName(), p.getProductDetailId());
            return "<div className='product-list grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 p-6'>" +
                    toProductCard(
                            p.getProductImage(),
                            p.getProductName(),
                            p.getPrice(),
                            p.getDescription(),
                            p.getCategoryName(),
                            p.getColorValue(),
                            p.getSizeValue(),
                            p.getWeightValue(),
                            p.getQuantity(),
                            p.getProductDetailId()
                    ) +
                    "</div>";
        }

        // Thử tìm trong danh mục liên quan
        String fallbackCategory = lower.contains("sữa tắm") || lower.contains("camellia") ? "phụ kiện" :
                lower.contains("bánh thưởng") || lower.contains("funny") ? "thức ăn" :
                        lower.contains("đệm") || lower.contains("nhà đệ") ? "chuồng" : null;
        if (fallbackCategory != null) {
            List<ProductDetailsDTO> relatedProducts = products.stream()
                    .filter(p -> p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(fallbackCategory))
                    .limit(3)
                    .collect(Collectors.toList());
            if (!relatedProducts.isEmpty()) {
                StringBuilder html = new StringBuilder("<p className='text-gray-500 mb-4'>Em chưa tìm thấy sản phẩm chính xác, nhưng đây là một số sản phẩm liên quan:</p>");
                html.append("<div className='product-list grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 p-6'>");
                for (ProductDetailsDTO p : relatedProducts) {
                    log.info("Related Product: Name={}, ProductDetailId={}", p.getProductName(), p.getProductDetailId());
                    html.append(toProductCard(
                            p.getProductImage(),
                            p.getProductName(),
                            p.getPrice(),
                            p.getDescription(),
                            p.getCategoryName(),
                            p.getColorValue(),
                            p.getSizeValue(),
                            p.getWeightValue(),
                            p.getQuantity(),
                            p.getProductDetailId()
                    ));
                }
                html.append("</div>");
                return html.toString();
            }
        }

        return "<p className='text-gray-500 mb-4'>Em chưa tìm thấy sản phẩm anh/chị yêu cầu. Anh/chị muốn em gợi ý thức ăn, đồ chơi, phụ kiện hay sữa tắm cho bé cưng không ạ?</p>";
    }

    // Hàm trích xuất tên sản phẩm từ tin nhắn của người dùng
    private String extractProductNameFromMessage(String userMessage) {
        String lower = userMessage.toLowerCase();
        for (String keyword : new String[]{"xem", "cho mèo", "cho chó", "tôi muốn"}) {
            lower = lower.replace(keyword, "");
        }
        return lower.trim();
    }

    // Hàm lấy thông tin cửa hàng (tên, giờ mở cửa, chính sách đổi trả, liên hệ)
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

    // Hàm tạo HTML cho một thẻ sản phẩm (product card)
    private String toProductCard(String img, String name, double price, String description, String category,
                                 String color, String size, float weight, int quantity, Long productDetailId) {
        if (price > 1_000_000) {
            log.warn("Sản phẩm {} có giá bất thường: {}", name, price);
        }
        if (productDetailId == null || productDetailId == 0) {
            log.error("ProductDetailId không hợp lệ cho sản phẩm {}: {}", name, productDetailId);
            productDetailId = 0L; // Giá trị mặc định nếu không hợp lệ
        }
        // Rút gọn description
        String shortDesc = description != null && description.length() > 60
                ? description.substring(0, 57) + "..."
                : description != null ? description : "Không có mô tả";
        // Thêm badge nếu sắp hết hàng
        String stockBadge = quantity < 5 && quantity > 0
                ? "<span className='absolute top-2 right-2 bg-red-500 text-white text-xs font-semibold px-2 py-1 rounded'>Sắp hết hàng</span>"
                : "";
        return String.format("""
            <div className='product-card bg-white shadow-md rounded-lg overflow-hidden relative hover:shadow-lg hover:scale-105 transition-transform duration-300'>
                %s
                <img src='%s' alt='%s' className='w-full h-48 object-cover'/>
                <div className='p-4'>
                    <h3 className='text-lg font-semibold text-gray-800 mb-2 truncate'>%s</h3>
                    <p className='text-gray-600 text-sm mb-2 line-clamp-2'>%s</p>
                    <p className='text-gray-800 font-medium mb-4'>Giá: %,.0f VND</p>
                </div>
            </div>
            """,
                stockBadge,
                img != null ? img : "https://via.placeholder.com/150",
                name != null ? name : "Không xác định",
                name != null ? name : "Không xác định",
                shortDesc,
                price,
                productDetailId
        );
    }

    // Hàm tạo JSON lỗi khi có vấn đề xảy ra
    private String jsonError(String message) {
        return String.format("{ \"status\": \"error\", \"message\": \"%s\" }", message);
    }

    // Enum định nghĩa các ý định (intent) của người dùng
    enum AiIntent {
        BEST_SELLERS,
        ALL_PRODUCTS,
        PRICE_FILTER_MIN,
        PRICE_FILTER_MAX,
        VIEW_MORE,
        PRODUCT_DETAIL,
        STORE_INFO,
        CATEGORY_FILTER,
        UNSUPPORTED_PET,
        DEFAULT
    }

    // Lớp lưu trữ ngữ cảnh hội thoại của người dùng
    @Data
    static class ConversationContext {
        private List<String> messageHistory = new ArrayList<>();
        private String lastCategory;
        private AiIntent lastIntent;
        private Map<String, Integer> pageOffsets = new HashMap<>();
        private Map<String, Long> displayedProducts = new HashMap<>();
        private Long userId;

        // Thêm tin nhắn vào lịch sử (giới hạn 10 tin nhắn)
        public void addMessage(String message) {
            messageHistory.add(message);
            if (messageHistory.size() > 10) {
                messageHistory.remove(0);
            }
        }

        // Lấy vị trí phân trang cho danh mục
        public int getPageOffset(String category) {
            return pageOffsets.getOrDefault(category, 0);
        }

        // Tăng vị trí phân trang
        public void incrementPageOffset(String category, int increment) {
            pageOffsets.put(category, pageOffsets.getOrDefault(category, 0) + increment);
        }

        // Đặt lại phân trang
        public void resetPagination() {
            pageOffsets.clear();
        }
    }
}