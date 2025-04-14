package org.example.petcarebe.repository;

import org.example.petcarebe.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VetPetRepository extends JpaRepository<Pet, Long> {
    // Bạn có thể thêm custom query nếu cần lọc theo điều kiện
}
