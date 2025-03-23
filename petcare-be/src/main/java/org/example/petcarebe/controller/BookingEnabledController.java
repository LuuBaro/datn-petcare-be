package org.example.petcarebe.controller;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.service.BookingEnabledService;
import org.example.petcarebe.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingEnabledController {

    @Autowired
    private BookingEnabledService bookingEnabledService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/booking-enabled")
    public ResponseEntity<Boolean> getBookingStatus() {
        BookingEnabled bookingEnabled = bookingEnabledService.getBookingStatus();
        return ResponseEntity.ok(bookingEnabled.isSettingValue());
    }

    @PutMapping("/staff/booking-enabled")
    public ResponseEntity<String> updateBookingStatus(
            @RequestParam("status") boolean status,
            Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new RuntimeException("Không tìm thấy token trong yêu cầu");
        }
        String token = authentication.getCredentials().toString();
        Long userId = jwtService.extractUserId(token);

        bookingEnabledService.updateBookingStatus(status, userId);
        return ResponseEntity.ok("Cập nhật trạng thái đặt lịch thành công");
    }
}