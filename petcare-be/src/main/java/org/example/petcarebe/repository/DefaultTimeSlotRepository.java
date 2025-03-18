package org.example.petcarebe.repository;

import org.example.petcarebe.model.DefaultTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DefaultTimeSlotRepository extends JpaRepository<DefaultTimeSlot, Long> {
    Optional<DefaultTimeSlot> findByTimeAndSlotIndex(LocalTime time, int slotIndex);
    List<DefaultTimeSlot> findByIsActiveTrue(); // Chỉ lấy các slot đang hoạt động
}
