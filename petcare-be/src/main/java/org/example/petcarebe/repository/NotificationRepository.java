package org.example.petcarebe.repository;

import org.example.petcarebe.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId); // Tìm tất cả thông báo của một user
    List<Notification> findByUserIdAndIsReadFalse(Long userId); // Tìm thông báo chưa đọc của một user
    void deleteByIsReadTrueAndTimestampBefore(LocalDateTime timestamp); // Thêm phương thức xóa
}
