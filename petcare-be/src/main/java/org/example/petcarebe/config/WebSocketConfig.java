package org.example.petcarebe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Bật simple broker cho cả /topic (broadcast) và /queue (tin nhắn cá nhân)
        config.enableSimpleBroker("/topic", "/queue");
        // Đặt tiền tố cho các đích user-specific là /user
        config.setUserDestinationPrefix("/user");
        // Đặt tiền tố cho các endpoint được ánh xạ từ @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173") // Cập nhật nếu frontend chạy trên cổng khác (ví dụ: 3000)
                .withSockJS();
    }
}