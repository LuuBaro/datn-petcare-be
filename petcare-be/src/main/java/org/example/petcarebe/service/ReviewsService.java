package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.model.Reviews;
import org.example.petcarebe.repository.ReviewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    // Lưu đánh giá
    public Reviews addReview(Reviews review) {
        return reviewsRepository.save(review);
    }

    // Lấy tất cả đánh giá của một sản phẩm (orderDetails)
    public List<Reviews> getReviewsByOrderDetails(Long orderDetailsId) {
        return reviewsRepository.findByOrderDetails_OrderDetailsId(orderDetailsId);
    }

    // Lấy tất cả đánh giá của một người dùng
    public List<Reviews> getReviewsByUser(Long userId) {
        return reviewsRepository.findByUser_UserId(userId);
    }


}
