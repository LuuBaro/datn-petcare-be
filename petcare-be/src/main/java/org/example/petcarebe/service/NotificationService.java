package org.example.petcarebe.service;

import org.example.petcarebe.model.Notification;
import org.example.petcarebe.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // Lưu thông báo mới
    public Notification saveNotification(Long userId, String message) {
        Notification notification = new Notification(userId, message);
        return notificationRepository.save(notification);
    }

    // Lấy tất cả thông báo của một user
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    // Lấy tất cả thông báo chưa đọc của một user
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    // Đánh dấu thông báo là đã đọc
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo với ID: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // Đánh dấu tất cả thông báo của một user là đã đọc
    public void markAllNotificationsAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    // Xóa các thông báo đã đọc sau 3 ngày
    public void deleteOldReadNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        notificationRepository.deleteByIsReadTrueAndTimestampBefore(threshold);
    }

    // Lên lịch chạy tự động mỗi ngày lúc 12h đêm (midnight)
    @Scheduled(cron = "0 0 0 * * ?") // Chạy mỗi ngày lúc 00:00
    public void scheduleDeleteOldReadNotifications() {
        deleteOldReadNotifications();
        System.out.println("Deleted old read notifications (older than 3 days)");
    }
}
