package org.example.petcarebe.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, String destination, String message) {
        String userDestination = "/user/" + userId + destination;
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), destination, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WebSocket message to user " + userId + ": " + e.getMessage(), e);
        }
    }

    public void sendToTopic(String topic, String message) {
        messagingTemplate.convertAndSend(topic, message);
    }

    public void sendToRole(String role, String destination, String message) {
        messagingTemplate.convertAndSend("/role/" + role + destination, message);
    }
}