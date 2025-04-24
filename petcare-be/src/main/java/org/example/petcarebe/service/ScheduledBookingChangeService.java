package org.example.petcarebe.service;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.model.ScheduledBookingChange;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.BookingEnabledRepository;
import org.example.petcarebe.repository.ScheduledBookingChangeRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledBookingChangeService {

    @Autowired
    private ScheduledBookingChangeRepository scheduledBookingChangeRepo;

    @Autowired
    private BookingEnabledRepository bookingEnabledRepo;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ScheduledBookingChange createSchedule(LocalDateTime scheduledTime, boolean status, boolean isRecurring, String recurrencePattern, String description, Long userId) {
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian thực hiện phải trong tương lai");
        }
        if (isRecurring && recurrencePattern == null) {
            throw new IllegalArgumentException("Chu kỳ lặp lại không được để trống khi lặp lại");
        }
        if (!isRecurring && recurrencePattern != null) {
            throw new IllegalArgumentException("Chu kỳ lặp lại phải để trống khi không lặp lại");
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        ScheduledBookingChange schedule = new ScheduledBookingChange();
        schedule.setScheduledTime(scheduledTime);
        schedule.setStatus(status);
        schedule.setRecurring(isRecurring);
        schedule.setRecurrencePattern(recurrencePattern);
        schedule.setDescription(description);
        schedule.setCreatedBy(user);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setExecuted(false);

        return scheduledBookingChangeRepo.save(schedule);
    }

    @Transactional
    public ScheduledBookingChange updateSchedule(Long id, LocalDateTime scheduledTime, boolean status, boolean isRecurring, String recurrencePattern, String description, Long userId) {
        ScheduledBookingChange schedule = scheduledBookingChangeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch không tồn tại"));
        if (schedule.isExecuted() && !schedule.isRecurring()) {
            throw new IllegalArgumentException("Không thể sửa lịch đã thực thi và không lặp lại");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        schedule.setScheduledTime(scheduledTime);
        schedule.setStatus(status);
        schedule.setRecurring(isRecurring);
        schedule.setRecurrencePattern(isRecurring ? recurrencePattern : null);
        schedule.setDescription(description);
        schedule.setCreatedBy(user);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setExecuted(false);

        return scheduledBookingChangeRepo.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        ScheduledBookingChange schedule = scheduledBookingChangeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch không tồn tại"));
        if (schedule.isExecuted() && !schedule.isRecurring()) {
            throw new IllegalArgumentException("Không thể xóa lịch đã thực thi và không lặp lại");
        }
        scheduledBookingChangeRepo.delete(schedule);
    }

    public List<ScheduledBookingChange> getAllSchedules() {
        return scheduledBookingChangeRepo.findAll();
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void processScheduledChanges() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledBookingChange> schedules = scheduledBookingChangeRepo.findByExecutedFalseAndScheduledTimeBefore(now);

        for (ScheduledBookingChange schedule : schedules) {
            // Cập nhật trạng thái hệ thống
            BookingEnabled bookingEnabled = new BookingEnabled();
            bookingEnabled.setSettingName("booking_enabled");
            bookingEnabled.setSettingValue(schedule.isStatus());
            bookingEnabled.setUpdatedAt(LocalDateTime.now());
            bookingEnabled.setUser(schedule.getCreatedBy());
            bookingEnabledRepo.save(bookingEnabled);

            // Xử lý lịch
            if (schedule.isRecurring()) {
                // Cập nhật scheduledTime cho lần chạy tiếp theo
                String pattern = schedule.getRecurrencePattern();
                LocalDateTime nextTime = schedule.getScheduledTime();
                switch (pattern.toUpperCase()) {
                    case "DAILY":
                        nextTime = nextTime.plusDays(1);
                        break;
                    case "WEEKLY":
                        nextTime = nextTime.plusWeeks(1);
                        break;
                    case "MONTHLY":
                        nextTime = nextTime.plusMonths(1);
                        break;
                    default:
                        throw new IllegalStateException("Chu kỳ lặp lại không hợp lệ: " + pattern);
                }
                schedule.setScheduledTime(nextTime);
                scheduledBookingChangeRepo.save(schedule);
            } else {
                // Đánh dấu lịch không lặp lại là đã thực thi
                schedule.setExecuted(true);
                scheduledBookingChangeRepo.save(schedule);
            }
        }
    }
}