package org.example.petcarebe.repository;

import org.example.petcarebe.dto.ProductSummaryDTO;
import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {
//    @Query("SELECT new org.example.petcarebe.dto.ProductsDTO(p.productId, p.productName, p.image, c.categoryName, d.brandName) " +
//            "FROM Products p " +
//            "JOIN p.categories c " +
//            "JOIN p.brand d")
//    List<ProductsDTO> findAllProductsWithMinPrice();

    List<Products> findByProductNameContainingIgnoreCase(String productName);

    @Query("SELECT new org.example.petcarebe.dto.ProductSummaryDTO(" +
            "dp.productDetailId, p.productName, p.image, dp.price, dp.quantity, p.description, b.brandName, c.categoryName) " +
            "FROM ProductDetails dp " +
            "JOIN dp.products p " +
            "JOIN p.brand b " +
            "JOIN p.categories c " +
            "WHERE p.productId = :productId")
    List<ProductSummaryDTO> findProductSummaryByProductId(@Param("productId") Long productId);

    @Query("SELECT new org.example.petcarebe.dto.ProductSummaryDTO(" +
            "dp.productDetailId, p.productName, p.image, dp.price, dp.quantity, p.description, b.brandName, c.categoryName) " +
            "FROM ProductDetails dp " +
            "JOIN dp.products p " +
            "JOIN p.brand b " +
            "JOIN p.categories c")
    List<ProductSummaryDTO> findAllProductSummaries();

}

