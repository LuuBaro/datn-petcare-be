package org.example.petcarebe.controller;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.service.BookingEnabledService;
import org.example.petcarebe.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookingEnabledController {

    @Autowired
    private BookingEnabledService bookingEnabledService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/booking-enabled")
    public ResponseEntity<?> getBookingStatus() {
        try {
            BookingEnabled bookingEnabled = bookingEnabledService.getBookingStatus();
            if (bookingEnabled == null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(true);
            }
            return ResponseEntity.ok(bookingEnabled.isSettingValue());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi lấy trạng thái đặt lịch: " + e.getMessage()));
        }
    }

    @PutMapping("/staff/booking-enabled")
    public ResponseEntity<?> updateBookingStatus(
            @RequestParam("status") boolean status,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getCredentials() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Không tìm thấy token trong yêu cầu"));
            }
            String token = authentication.getCredentials().toString();
            Long userId = jwtService.extractUserId(token);

            bookingEnabledService.updateBookingStatus(status, userId);
            return ResponseEntity.ok(new SuccessResponse("Cập nhật trạng thái đặt lịch thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi cập nhật trạng thái đặt lịch: " + e.getMessage()));
        }
    }

    public static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

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