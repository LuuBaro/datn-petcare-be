package org.example.petcarebe.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @MessageMapping("/sendMessage") // Client gửi tin nhắn đến đây
    @SendTo("/topic/public") // Broadcast đến tất cả các client đang subscribe
    public String sendMessage(String message) {
        return message;
    }
}
