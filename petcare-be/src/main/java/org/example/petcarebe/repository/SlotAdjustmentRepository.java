package org.example.petcarebe.repository;

import org.example.petcarebe.model.SlotAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotAdjustmentRepository extends JpaRepository<SlotAdjustment, Long> {
    List<SlotAdjustment> findByDate(LocalDate date);
}