package org.example.petcarebe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
@Service
public class WebSocketService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, String destination, String message) {
        String userDestination = "/user/" + userId + destination;
        System.out.println("🔔 Attempting to send WebSocket message to: " + userDestination);
        System.out.println("📨 Message: " + message);
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), destination, message);
            System.out.println("✅ Message sent successfully to: " + userDestination);
        } catch (Exception e) {
            System.err.println("❌ Error sending WebSocket message: " + e.getMessage());
        }
    }

    public void sendToTopic(String topic, String message) {
        messagingTemplate.convertAndSend(topic, message);
    }

    public void sendToRole(String role, String destination, String message) {
        messagingTemplate.convertAndSend("/role/" + role + destination, message);
    }
}