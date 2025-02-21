package org.example.petcarebe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.*;

@Service
public class AiService {
    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyD9O2X5xmIpB8as6vT5aeZIS5fJ0Jvdsxg";

    @Autowired
    private ProductsService productsService;

    public String getAiResponse(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        // 🔹 Lấy danh sách sản phẩm từ hệ thống
        Map<String, Object> data = new HashMap<>();
        data.put("allProducts", productsService.getAllProductss());

        // 🔹 Chuyển danh sách sản phẩm thành JSON
        String productsJson;
        try {
            productsJson = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            productsJson = "{}";
        }

        // 🔹 Tạo prompt cho AI
        String prompt = "Bạn là nhân viên tư vấn của PetCare. Đây là danh sách sản phẩm:\n\n"
                + productsJson
                + "\n\nNgười dùng hỏi: " + userMessage + "\nHãy trả lời chi tiết, rõ ràng và dễ đọc.";

        // 🔹 Chuẩn bị request JSON hợp lệ
        Map<String, Object> requestBodyMap = new HashMap<>();
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", prompt);
        requestBodyMap.put("contents", new Object[]{Map.of("parts", new Object[]{contentPart})});

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestBodyMap);
        } catch (Exception e) {
            return "Lỗi chuyển đổi JSON!";
        }

        // 🔹 Cấu hình headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 🔹 Gửi request đến Gemini API
        ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, entity, String.class);
        System.out.println("AI chat: "+response.getBody());

        return formatAiResponse(response.getBody());
    }

    private String formatAiResponse(String aiResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Format JSON đẹp hơn

        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            // 🔹 Lấy phần phản hồi chính từ AI
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                return "{ \"error\": \"Không có phản hồi từ AI.\" }";
            }

            JsonNode contentParts = candidates.get(0).path("content").path("parts");
            if (!contentParts.isArray() || contentParts.size() == 0) {
                return "{ \"error\": \"Không tìm thấy nội dung phản hồi.\" }";
            }

            // 🔹 Lấy nội dung text AI trả lời
            String responseText = contentParts.get(0).path("text").asText();

            // 🔹 Chuẩn bị JSON đẹp để hiển thị trên Postman
            Map<String, Object> formattedResponse = new LinkedHashMap<>();
            formattedResponse.put("status", "success");
            formattedResponse.put("message", "Phản hồi từ AI");
            formattedResponse.put("response", responseText);
            formattedResponse.put("timestamp", new Date().toString());

            return objectMapper.writeValueAsString(formattedResponse);

        } catch (Exception e) {
            return "{ \"error\": \"Lỗi khi xử lý phản hồi từ AI!\" }";
        }
    }






}
