// AppointmentRepository.java
package org.example.petcarebe.repository;

import org.example.petcarebe.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("SELECT COUNT(p) FROM Appointment a JOIN a.pets p WHERE a.date = :date AND a.time = :time AND a.status IN ('PAID', 'CONFIRMED')")
    long countPetsByDateAndTimeAndStatus(LocalDate date, LocalTime time);

    @Query("SELECT COUNT(p) FROM Appointment a JOIN a.pets p WHERE a.date = :date AND a.time = :time AND a.status NOT IN ('CANCELLED')")
    long countPetsByDateAndTime(LocalDate date, LocalTime time);
}