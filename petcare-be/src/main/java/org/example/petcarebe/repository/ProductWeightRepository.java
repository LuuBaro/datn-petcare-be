package org.example.petcarebe.repository;


import org.example.petcarebe.model.Weights;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductWeightRepository extends JpaRepository<Weights, Long> {
}
