package org.example.petcarebe.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // ID của người dùng nhận thông báo

    @Column(nullable = false)
    private String message; // Nội dung thông báo

    @Column(nullable = false)
    private boolean isRead = false; // Trạng thái đã đọc/chưa đọc

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now(); // Thời gian tạo thông báo

    // Constructors
    public Notification() {}

    public Notification(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
