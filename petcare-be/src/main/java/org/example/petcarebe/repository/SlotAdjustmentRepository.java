package org.example.petcarebe.repository;

import org.example.petcarebe.model.SlotAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface SlotAdjustmentRepository extends JpaRepository<SlotAdjustment, Long> {
    Optional<SlotAdjustment> findByDateAndTimeAndIsPermanent(LocalDate date, LocalTime time, boolean isPermanent);
}