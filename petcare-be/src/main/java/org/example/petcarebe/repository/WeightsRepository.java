package org.example.petcarebe.repository;


import org.example.petcarebe.model.Weights;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightsRepository extends JpaRepository<Weights, Long> {
}
