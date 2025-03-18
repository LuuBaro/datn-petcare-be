package org.example.petcarebe.repository;

import org.example.petcarebe.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    long countByDateAndTime(LocalDate date, LocalTime time);
}