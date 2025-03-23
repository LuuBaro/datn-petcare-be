package org.example.petcarebe.service;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.BookingEnabledRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BookingEnabledService {

    @Autowired
    private BookingEnabledRepository bookingEnabledRepository;

    @Autowired
    private UserRepository userRepository;

    public BookingEnabled getBookingStatus() {
        return bookingEnabledRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new RuntimeException("Booking status not found"));
    }

    public void updateBookingStatus(boolean status, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BookingEnabled bookingEnabled = new BookingEnabled();
        bookingEnabled.setSettingName("booking_enabled");
        bookingEnabled.setSettingValue(status);
        bookingEnabled.setUpdatedAt(LocalDateTime.now());
        bookingEnabled.setUser(user);

        bookingEnabledRepository.save(bookingEnabled);
    }
}