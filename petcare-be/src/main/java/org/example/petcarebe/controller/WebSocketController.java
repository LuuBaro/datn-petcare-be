package org.example.petcarebe.controller;

import org.example.petcarebe.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ws")
public class WebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private WebSocketService webSocketService;

    public void sendNotificationToUser(Long userId, String message) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", message);
    }

    @MessageMapping("/sendMessage")
    public void sendMessage(String message) {
        webSocketService.sendToTopic("/topic/public", "Broadcast: " + message);
    }

    @GetMapping("/test-websocket")
    public ResponseEntity<String> testWebSocket(@RequestParam Long userId, @RequestParam String message) {
        webSocketService.sendToUser(userId, "/queue/notifications", message); // Sử dụng sendToUser thay vì convertAndSend
        return ResponseEntity.ok("Message sent to user " + userId + ": " + message);
    }
}