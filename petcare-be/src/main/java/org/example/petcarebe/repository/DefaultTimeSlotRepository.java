package org.example.petcarebe.repository;

import org.example.petcarebe.model.DefaultTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface DefaultTimeSlotRepository extends JpaRepository<DefaultTimeSlot, Long> {
    Optional<DefaultTimeSlot> findByTime(LocalTime time);
}