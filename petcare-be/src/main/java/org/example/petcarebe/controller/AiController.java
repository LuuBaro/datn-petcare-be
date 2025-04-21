package org.example.petcarebe.controller;

import org.example.petcarebe.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAi(@RequestBody Map<String, String> request) {
        // Kiểm tra xác thực
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("response", "Vui lòng đăng nhập để sử dụng chatbot."));
        }

        // Lấy userId và message từ request body
        String userIdStr = request.get("userId");
        String userMessage = request.get("message");

        if (userIdStr == null || userMessage == null) {
            return ResponseEntity.status(400).body(Map.of("response", "Thiếu userId hoặc message trong request."));
        }

        // Chuyển userId từ String sang Long
        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body(Map.of("response", "userId phải là một số hợp lệ."));
        }

        // Gọi AiService với userMessage, userName, và userId
        String aiResponse = aiService.getAiResponse(userMessage, "Khách hàng " + userId, userId);

        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}