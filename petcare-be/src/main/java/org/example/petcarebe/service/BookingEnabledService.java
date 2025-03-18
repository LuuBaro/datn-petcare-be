package org.example.petcarebe.service;

import org.example.petcarebe.model.BookingEnabled;
import org.example.petcarebe.repository.BookingEnabledRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class BookingEnabledService {

    @Autowired
    private BookingEnabledRepository bookingEnabledRepository;

    public BookingEnabled getBookingStatus() {
        BookingEnabled bookingEnabled = bookingEnabledRepository.findBySettingName("ENABLE_BOOKING");
        if (bookingEnabled == null) {
            bookingEnabled = new BookingEnabled();
            bookingEnabled.setSettingName("ENABLE_BOOKING");
            bookingEnabled.setSettingValue(true);
            bookingEnabled.setUpdatedAt(LocalDateTime.now());
            bookingEnabled.setUser(null); // Hiện tại không cần user
            bookingEnabled = bookingEnabledRepository.save(bookingEnabled);
        }
        return bookingEnabled;
    }

    public BookingEnabled updateBookingStatus(boolean newStatus) {
        BookingEnabled bookingEnabled = bookingEnabledRepository.findBySettingName("ENABLE_BOOKING");
        if (bookingEnabled == null) {
            bookingEnabled = new BookingEnabled();
            bookingEnabled.setSettingName("ENABLE_BOOKING");
        }

        bookingEnabled.setSettingValue(newStatus);
        bookingEnabled.setUpdatedAt(LocalDateTime.now());
        bookingEnabled.setUser(null); // Hiện tại không cần user
        return bookingEnabledRepository.save(bookingEnabled);
    }
}