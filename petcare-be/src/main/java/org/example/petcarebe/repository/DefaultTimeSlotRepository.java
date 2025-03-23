// DefaultTimeSlotRepository.java
package org.example.petcarebe.repository;

import org.example.petcarebe.model.DefaultTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultTimeSlotRepository extends JpaRepository<DefaultTimeSlot, Long> {
    List<DefaultTimeSlot> findByIsActiveTrueOrderBySlotIndexAsc();
}