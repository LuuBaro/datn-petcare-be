package org.example.petcarebe.repository;

import org.example.petcarebe.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAppointmentAppointmentId(Long appointmentId);
}