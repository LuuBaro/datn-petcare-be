package org.example.petcarebe.repository;

import org.example.petcarebe.model.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    Optional<AppointmentSlot> findByDateAndTime(LocalDate date, LocalTime time);
    List<AppointmentSlot> findByDate(LocalDate date);
}