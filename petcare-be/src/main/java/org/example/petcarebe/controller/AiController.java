package org.example.petcarebe.controller;

import org.example.petcarebe.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    @Autowired
    private AiService aiService;

    // 🔹 API chat với AI
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAi(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message"); // Lấy tin nhắn người dùng
        String aiResponse = aiService.getAiResponse(userMessage); // Gửi tới AI

        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}
