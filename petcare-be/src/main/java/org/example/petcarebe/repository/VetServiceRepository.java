package org.example.petcarebe.repository;

import org.example.petcarebe.enums.PetType;
import org.example.petcarebe.model.VetService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VetServiceRepository extends JpaRepository<VetService, Long> {
    List<VetService> findByActiveTrue();

    List<VetService> findByPetType(PetType petType);
}
