package org.example.petcarebe.repository;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.enums.StatusType;
import org.example.petcarebe.model.PetService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetServiceRepository extends JpaRepository<PetService, Long> {
    List<PetService> findByPetTypeAndStatusType(PetType petType, StatusType statusType);
}