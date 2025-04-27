package org.example.petcarebe.service;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.BookingEnabledRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class BookingEnabledService {

    @Autowired
    private BookingEnabledRepository bookingEnabledRepository;

    @Autowired
    private UserRepository userRepository;

    public BookingEnabled getBookingStatus() {
        Optional<BookingEnabled> bookingEnabled = bookingEnabledRepository.findTopByOrderByUpdatedAtDesc();
        return bookingEnabled.orElse(null);
    }

    public void updateBookingStatus(boolean status, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        BookingEnabled bookingEnabled = new BookingEnabled();
        bookingEnabled.setSettingName("booking_enabled");
        bookingEnabled.setSettingValue(status);
        bookingEnabled.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        bookingEnabled.setUser(user);

        bookingEnabledRepository.save(bookingEnabled);
    }
}