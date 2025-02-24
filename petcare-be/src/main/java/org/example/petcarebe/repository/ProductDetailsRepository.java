package org.example.petcarebe.repository;


import jakarta.transaction.Transactional;
import org.example.petcarebe.dto.ProductDetailsDTO;
import org.example.petcarebe.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, Long> {

    @Modifying
    @Query("UPDATE ProductDetails p SET p.quantity = p.quantity - :quantity WHERE p.productDetailId = :productDetailId AND p.quantity >= :quantity")
    int updateStock(@Param("productDetailId") Long productDetailId, @Param("quantity") int quantity);

    @Transactional
    @Modifying
    @Query(value = "UPDATE product_details SET quantity = quantity + :quantity WHERE product_detail_id = :productDetailId", nativeQuery = true)
    int updateStockcancel(@Param("productDetailId") Long productDetailId, @Param("quantity") int quantity);

    // Fetch product details by ID
    @Query("SELECT new org.example.petcarebe.dto.ProductDetailsDTO(" +
            "dp.productDetailId, p.productName,p.image, dp.price, pc.colorValue, ps.sizeValue, w.weightValue, dp.quantity, p.description) " +
            "FROM ProductDetails dp " +
            "JOIN dp.products p " +
            "JOIN dp.productColors pc " +
            "JOIN dp.productSizes ps " +
            "JOIN dp.weights w " +
            "WHERE dp.productDetailId = :productDetailId")
    Optional<ProductDetailsDTO> findByProductDetailId(Long productDetailId);

    // Fetch all product details
    @Query("SELECT new org.example.petcarebe.dto.ProductDetailsDTO(" +
            "dp.productDetailId, p.productName,p.image, dp.price, pc.colorValue, ps.sizeValue, w.weightValue, dp.quantity, p.description) " +
            "FROM ProductDetails dp " +
            "JOIN dp.products p " +
            "JOIN dp.productColors pc " +
            "JOIN dp.productSizes ps " +
            "JOIN dp.weights w")
    List<ProductDetailsDTO> findAllProductDetails();

    // Fetch product detail by color, size, and weight
    @Query("SELECT pd FROM ProductDetails pd " +
            "JOIN pd.productColors pc " +
            "JOIN pd.productSizes ps " +
            "JOIN pd.weights w " +
            "WHERE pc.colorValue = :colorValue " +
            "AND ps.sizeValue = :sizeValue " +
            "AND w.weightValue = :weightValue")
    Optional<ProductDetails> findByColorSizeWeight(String colorValue, String sizeValue, String weightValue);


    @Query("SELECT MIN(pd.price) FROM ProductDetails pd WHERE pd.products.productId = :productId")
    Float findMinPriceByProductId(@Param("productId") Long productId);

    // Tìm tất cả ProductDetails theo productId
    @Query("SELECT pd FROM ProductDetails pd WHERE pd.products.productId = :productId")
    List<ProductDetails> findByProductId(@Param("productId") Long productId);


    @Query("SELECT COALESCE(SUM(pd.quantity), 0) FROM ProductDetails pd")
    int getTotalStock();


    @Query("SELECT pd, COALESCE(SUM(pd.quantity), 0) FROM ProductDetails pd " +
            "GROUP BY pd.productDetailId, pd.products.productId, pd.products.productName, " +
            "pd.price, pd.productColors.colorValue, pd.productSizes.sizeValue, pd.weights.weightValue, pd.products.image")
    List<Object[]> findProductStockInfo();


    // Tìm tất cả ProductDetails theo productId
    @Query("SELECT p, MIN(pd.price) " +
            "FROM Products p " +
            "JOIN p.productDetails pd " +
            "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')) " +
            "GROUP BY p")
    List<Object[]> searchProductsWithPrice(@Param("productName") String productName);





}
