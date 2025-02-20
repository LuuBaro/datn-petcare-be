package org.example.petcarebe.repository;

import org.example.petcarebe.model.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewsRepository extends JpaRepository<Reviews, Long> {
    // Tìm đánh giá theo orderDetailsId
    List<Reviews> findByOrderDetails_OrderDetailsId(Long orderDetailsId);

    // Tìm đánh giá theo userId
    List<Reviews> findByUser_UserId(Long userId);
}
