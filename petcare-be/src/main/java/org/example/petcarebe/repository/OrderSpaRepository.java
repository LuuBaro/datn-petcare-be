package org.example.petcarebe.repository;

import org.example.petcarebe.model.OrderSpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderSpaRepository extends JpaRepository<OrderSpa, Long> {
    List<OrderSpa> findByOrderOrderId(Long orderId);
    List<OrderSpa> findByPetId(Long petId);
} 