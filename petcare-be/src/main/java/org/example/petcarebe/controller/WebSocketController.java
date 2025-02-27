package org.example.petcarebe.controller;

import org.example.petcarebe.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequestMapping("/api/ws")
public class WebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private WebSocketService webSocketService;
    // Gửi thông báo đến một userId cụ thể
    public void sendNotificationToUser(Long userId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }

    // Nhận tin nhắn từ client và broadcast (chỉ để test hoặc dùng nếu cần)
    @MessageMapping("/sendMessage") // Client gửi tin nhắn đến /app/sendMessage
    public void sendMessage(String message) {
        webSocketService.sendToTopic("/topic/public", "Broadcast: " + message);
    }

}
