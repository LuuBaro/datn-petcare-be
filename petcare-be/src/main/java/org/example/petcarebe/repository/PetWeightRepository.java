package org.example.petcarebe.repository;

import org.example.petcarebe.model.PetWeight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetWeightRepository extends JpaRepository<PetWeight, Long> {
}