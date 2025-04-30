package org.example.petcarebe.repository;

import org.example.petcarebe.model.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {
}