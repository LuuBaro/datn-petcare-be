package org.example.petcarebe.repository;

import org.example.petcarebe.model.PetWeight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VetPetWeightRepository extends JpaRepository<PetWeight, Long> {
    // Các phương thức tùy chỉnh nếu cần
}
