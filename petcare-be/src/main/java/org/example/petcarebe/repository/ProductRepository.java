package org.example.petcarebe.repository;


import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {
    @Query("SELECT new org.example.petcarebe.dto.ProductsDTO(p.productid, p.productname, p.image) " +
            "FROM Products p" )
    List<ProductsDTO> findAllProductsWithMinPrice();
}
