package org.example.petcarebe.repository;

import org.example.petcarebe.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {
}