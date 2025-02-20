package org.example.petcarebe.repository;

import org.example.petcarebe.model.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewsRepository extends JpaRepository<Reviews, Long> {
    // Tìm đánh giá theo orderDetailsId
    List<Reviews> findByOrderDetails_OrderDetailsId(Long orderDetailsId);

    // Tìm đánh giá theo userId
    List<Reviews> findByUser_UserId(Long userId);

    // Lấy tất cả đánh giá theo productDetailId (tìm orderDetail trước)
    @Query("SELECT r FROM Reviews r WHERE r.orderDetails.productDetails.productDetailId = :productDetailId")
    List<Reviews> findByProductDetailId(@Param("productDetailId") Long productDetailId);
}
