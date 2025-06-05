package org.example.petcarebe.controller;

import org.example.petcarebe.model.ScheduledBookingChange;
import org.example.petcarebe.service.JwtService;
import org.example.petcarebe.service.ScheduledBookingChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/staff", produces = MediaType.APPLICATION_JSON_VALUE)
public class ScheduledBookingChangeController {

    @Autowired
    private ScheduledBookingChangeService scheduledBookingChangeService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/schedule-booking-enabled")
    public ResponseEntity<?> createSchedule(
            @RequestBody ScheduleRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getCredentials() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Không tìm thấy token trong yêu cầu"));
            }
            String token = authentication.getCredentials().toString();
            Long userId;
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Token không hợp lệ: " + e.getMessage()));
            }

            ScheduledBookingChange schedule = scheduledBookingChangeService.createSchedule(
                    request.getScheduledTime(),
                    request.getStatus(),
                    request.isRecurring(),
                    request.getRecurrencePattern(),
                    request.getDescription(),
                    userId
            );
            return ResponseEntity.ok(schedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi tạo lịch: " + e.getMessage()));
        }
    }

    @GetMapping("/scheduled-booking-changes")
    public ResponseEntity<?> getAllSchedules() {
        try {
            List<ScheduledBookingChange> schedules = scheduledBookingChangeService.getAllSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi lấy danh sách lịch: " + e.getMessage()));
        }
    }

    @PutMapping("/scheduled-booking-changes/{id}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Long id,
            @RequestBody ScheduleRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getCredentials() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Không tìm thấy token trong yêu cầu"));
            }
            String token = authentication.getCredentials().toString();
            Long userId;
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Token không hợp lệ: " + e.getMessage()));
            }

            ScheduledBookingChange updatedSchedule = scheduledBookingChangeService.updateSchedule(
                    id,
                    request.getScheduledTime(),
                    request.getStatus(),
                    request.isRecurring(),
                    request.getRecurrencePattern(),
                    request.getDescription(),
                    userId
            );
            return ResponseEntity.ok(updatedSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi cập nhật lịch: " + e.getMessage()));
        }
    }

    @DeleteMapping("/scheduled-booking-changes/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            scheduledBookingChangeService.deleteSchedule(id);
            return ResponseEntity.ok(new SuccessResponse("Xóa lịch thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi xóa lịch: " + e.getMessage()));
        }
    }

    // DTO cho request
    public static class ScheduleRequest {
        private LocalDateTime scheduledTime;
        private boolean status;
        private boolean isRecurring;
        private String recurrencePattern;
        private String description;

        public LocalDateTime getScheduledTime() {
            return scheduledTime;
        }

        public void setScheduledTime(LocalDateTime scheduledTime) {
            this.scheduledTime = scheduledTime;
        }

        public boolean getStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }

        public boolean isRecurring() {
            return isRecurring;
        }

        public void setRecurring(boolean recurring) {
            isRecurring = recurring;
        }

        public String getRecurrencePattern() {
            return recurrencePattern;
        }

        public void setRecurrencePattern(String recurrencePattern) {
            this.recurrencePattern = recurrencePattern;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // DTO cho phản hồi lỗi
    public static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    // DTO cho phản hồi thành công
    public static class SuccessResponse {
        private final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}