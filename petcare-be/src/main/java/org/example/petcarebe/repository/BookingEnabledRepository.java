package org.example.petcarebe.repository;

import org.example.petcarebe.model.BookingEnabled;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingEnabledRepository extends JpaRepository<BookingEnabled, Long> {
    Optional<BookingEnabled> findTopByOrderByUpdatedAtDesc();
}