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
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Định nghĩa các prefix mà client có thể subscribe
        registry.enableSimpleBroker("/topic", "/queue"); // /topic cho broadcast, /queue cho cá nhân
        registry.setApplicationDestinationPrefixes("/app"); // Prefix cho các @MessageMapping
        registry.setUserDestinationPrefix("/user"); // Prefix cho tin nhắn cá nhân (/user/{userId}/queue/...)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Định nghĩa endpoint mà client kết nối
        registry.addEndpoint("/ws") // Đường dẫn WebSocket: ws://localhost:8080/ws
                .setAllowedOrigins("*") // Cho phép mọi nguồn (thay bằng domain cụ thể trong production)
                .withSockJS(); // Hỗ trợ SockJS cho trình duyệt không hỗ trợ WebSocket
    }
}
