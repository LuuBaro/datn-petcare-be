package org.example.petcarebe.repository;

import org.example.petcarebe.model.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface VetPetRepository extends JpaRepository<Pet, Long> {
    Page<Pet> findAllByDeletedFalse(Pageable pageable);




}
