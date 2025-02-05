package org.example.petcarebe.repository;


import org.example.petcarebe.dto.ProductDetailsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, Long> {

    // Fetch product details by ID
    @Query("SELECT new org.example.petcarebe.dto.ProductDetailsDTO(" +
            "dp.productDetailId, p.productName, dp.price, pc.colorValue, ps.sizeValue, w.weightValue, dp.quantity, p.description) " +
            "FROM ProductDetails dp " +
            "JOIN dp.products p " +
            "JOIN dp.productColors pc " +
            "JOIN dp.productSizes ps " +
            "JOIN dp.weights w " +
            "WHERE dp.productDetailId = :productDetailId")
    Optional<ProductDetailsDTO> findByProductDetailId(Long productDetailId);

    // Fetch all product details
    @Query("SELECT new org.example.petcarebe.dto.ProductDetailsDTO(" +
            "dp.productDetailId, p.productName, dp.price, pc.colorValue, ps.sizeValue, w.weightValue, dp.quantity, p.description) " +
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



//    // Truy vấn để lấy danh sách giá của sản phẩm theo productId
//    @Query("SELECT pd.price FROM ProductDetails pd WHERE pd.products.productid = :productId")
//    List<Float> findPricesByProductId(@Param("productId") Long productId);
//
//    // Truy vấn để lấy giá thấp nhất của sản phẩm theo productId
//    @Query("SELECT MIN(pd.price) FROM ProductDetails pd WHERE pd.products.productid = :productId")
//    Float findMinPriceByProductId(@Param("productId") Long productId);
//
//    // Truy vấn để lấy giá cao nhất của sản phẩm theo productId
//    @Query("SELECT MAX(pd.price) FROM ProductDetails pd WHERE pd.products.productid = :productId")
//    Float findMaxPriceByProductId(@Param("productId") Long productId);

    @Query("SELECT MIN(pd.price) FROM ProductDetails pd WHERE pd.products.productId = :productId")
    Float findMinPriceByProductId(@Param("productId") Long productId);

    // Tìm tất cả ProductDetails theo productId
    @Query("SELECT pd FROM ProductDetails pd WHERE pd.products.productId = :productId")
    List<ProductDetails> findByProductId(@Param("productId") Long productId);


}
