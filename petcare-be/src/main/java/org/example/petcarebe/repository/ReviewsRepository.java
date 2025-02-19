package org.example.petcarebe.repository;

import org.example.petcarebe.model.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    // Lấy tất cả đánh giá của sản phẩm theo productId
    List<Reviews> findByProductDetails_ProductDetailId(Long productId);

    // Lấy tất cả đánh giá của người dùng theo userId
    List<Reviews> findByUser_UserId(Long userId);

    // Lấy đánh giá theo productDetailId và userId
    Reviews findByProductDetails_ProductDetailIdAndUser_UserId(Long productDetailId, Long userId);
}
