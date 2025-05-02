package org.example.petcarebe.repository;

import org.example.petcarebe.model.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {

    @Query("SELECT h FROM AppointmentHistory h JOIN h.appointment a WHERE a.phone = :phone ORDER BY h.timestamp DESC")
    List<AppointmentHistory> findByPhone(@Param("phone") String phone);

    @Query("SELECT h FROM AppointmentHistory h ORDER BY h.timestamp DESC")
    List<AppointmentHistory> findAll();
}