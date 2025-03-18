package org.example.petcarebe.repository;

import org.example.petcarebe.model.BookingEnabled;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingEnabledRepository extends JpaRepository<BookingEnabled, Long> {
    BookingEnabled findBySettingName(String settingName);
}