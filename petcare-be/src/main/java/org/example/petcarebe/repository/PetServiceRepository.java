package org.example.petcarebe.repository;

import org.example.petcarebe.model.PetService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetServiceRepository extends JpaRepository<PetService, Long> {
}