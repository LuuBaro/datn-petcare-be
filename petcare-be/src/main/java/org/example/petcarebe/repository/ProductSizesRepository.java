package org.example.petcarebe.repository;


import org.example.petcarebe.model.ProductSizes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSizesRepository extends JpaRepository<ProductSizes, Long> {
}
