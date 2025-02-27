package org.example.petcarebe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Gửi thông báo đến một user cụ thể
    public void sendToUser(Long userId, String destination, String message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                destination, // Ví dụ: /queue/notifications
                message
        );
    }

    // Gửi thông báo broadcast đến một topic
    public void sendToTopic(String topic, String message) {
        messagingTemplate.convertAndSend(topic, message); // Ví dụ: /topic/public
    }

    // Gửi thông báo đến một vai trò (role)
    public void sendToRole(String role, String destination, String message) {
        messagingTemplate.convertAndSend("/role/" + role + destination, message); // Ví dụ: /role/ADMIN/queue/updates
    }
}
