package org.example.petcarebe.repository;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import org.example.petcarebe.model.PetWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetWeightRepository extends JpaRepository<PetWeight, Long> {
    List<PetWeight> findByPetTypeAndStatusType(PetType petType, StatusType statusType);
}