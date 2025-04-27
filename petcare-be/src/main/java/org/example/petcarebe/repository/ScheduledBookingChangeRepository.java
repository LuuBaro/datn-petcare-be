package org.example.petcarebe.repository;

import org.example.petcarebe.model.ScheduledBookingChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledBookingChangeRepository extends JpaRepository<ScheduledBookingChange, Long> {
    List<ScheduledBookingChange> findByExecutedFalseAndScheduledTimeBefore(LocalDateTime now);

    Optional<ScheduledBookingChange> findFirstByExecutedTrueAndScheduledTimeBeforeOrderByScheduledTimeDesc(LocalDateTime now);

    Optional<ScheduledBookingChange> findFirstByExecutedFalseAndScheduledTimeAfterOrderByScheduledTimeAsc(LocalDateTime now);
}