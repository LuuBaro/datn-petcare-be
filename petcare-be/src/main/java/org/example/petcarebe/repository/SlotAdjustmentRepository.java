// SlotAdjustmentRepository.java
package org.example.petcarebe.repository;

import org.example.petcarebe.model.SlotAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotAdjustmentRepository extends JpaRepository<SlotAdjustment, Long> {
    List<SlotAdjustment> findByTimeAndIsPermanentTrue(LocalTime time);
    List<SlotAdjustment> findByDateAndTimeAndIsPermanentFalse(LocalDate date, LocalTime time);
}