package org.example.petcarebe.controller;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.service.BookingEnabledService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking-enabled")
public class BookingEnabledController {

    @Autowired
    private BookingEnabledService bookingEnabledService;

    @GetMapping
    public ResponseEntity<BookingEnabled> getBookingStatus() {
        BookingEnabled bookingEnabled = bookingEnabledService.getBookingStatus();
        return ResponseEntity.ok(bookingEnabled);
    }

    @PutMapping
    public ResponseEntity<BookingEnabled> updateBookingStatus(@RequestParam boolean status) {
        BookingEnabled updatedBookingEnabled = bookingEnabledService.updateBookingStatus(status);
        return ResponseEntity.ok(updatedBookingEnabled);
    }
}