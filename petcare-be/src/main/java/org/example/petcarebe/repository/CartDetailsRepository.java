package org.example.petcarebe.repository;

import jakarta.transaction.Transactional;
import org.example.petcarebe.dto.CartDetailsDTO;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.example.petcarebe.model.CartDetails;

import java.util.List;

@Repository
public interface CartDetailsRepository extends JpaRepository<CartDetails, Long> {
    List<CartDetails> findByUserUserId(Long userId);
    @Query(value = """
        SELECT 
            c.cart_detail_id AS cartDetailId,
            p.product_detail_id AS productDetailId, 
            prod.image AS image,
            prod.product_name AS productName, 
            p.price AS price, 
            pc.color_value AS colorValue, 
            ps.size_value AS sizeValue, 
            w.weight_value AS weightValue, 
            c.quantity_item AS quantityItem, 
            prod.description AS description
        FROM cart_detail c
        JOIN product_details p ON c.product_detail_id = p.product_detail_id
        JOIN products prod ON p.product_id = prod.product_id
        JOIN product_colors pc ON p.product_color_id = pc.product_color_id
        JOIN product_sizes ps ON p.product_size_id = ps.product_size_id
        JOIN weights w ON p.weight_id = w.weight_id
        WHERE c.user_id = :userId
    """, nativeQuery = true)
    List<Object[]> findRawCartDetailsByUserId(@Param("userId") Long userId);

    public List<CartDetails> findByUser_UserId(Long userId);
    @Modifying
    @Transactional
    @Query("DELETE FROM CartDetails c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // 🔥 Tìm ProductDetails theo productDetailId trong giỏ hàng của user
    @Query("SELECT c.productDetails FROM CartDetails c WHERE c.productDetails.productDetailId = :productDetailId AND c.user.id = :userId")
    ProductDetails findProductDetailById(@Param("productDetailId") Long productDetailId, @Param("userId") Long userId);

    void deleteAllByUser_UserId(Long userId);

    CartDetails findByUserAndProductDetails(User user, ProductDetails productDetails);

    @Modifying
    @Query("DELETE FROM CartDetails c WHERE c.user.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM petcare.cart_detail WHERE product_detail_id = :productDetailId", nativeQuery = true)
    void deleteCartItemByProductDetailId(@Param("productDetailId") Long productDetailId);


    List<CartDetails> findByUser(User user);


    // Thêm phương thức để tìm CartDetails theo productDetailId
    @Query("SELECT c FROM CartDetails c WHERE c.productDetails.productDetailId = :productDetailId")
    List<CartDetails> findByProductDetails_ProductDetailId(@Param("productDetailId") Long productDetailId);

}
