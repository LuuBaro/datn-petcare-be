package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.model.Reviews;
import org.example.petcarebe.repository.ReviewsRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewsService {

    private final ReviewsRepository reviewsRepository;

    // Phương thức lấy tất cả đánh giá của sản phẩm
    public List<Reviews> getReviewsByProductDetailId(Long productDetailId) {
        return reviewsRepository.findByProductDetails_ProductDetailId(productDetailId);
    }

    // Phương thức lấy tất cả đánh giá của người dùng
    public List<Reviews> getReviewsByUserId(Long userId) {
        return reviewsRepository.findByUser_UserId(userId);
    }

    // Phương thức lấy đánh giá của một sản phẩm từ một người dùng
    public Reviews getReviewByProductDetailIdAndUserId(Long productDetailId, Long userId) {
        return reviewsRepository.findByProductDetails_ProductDetailIdAndUser_UserId(productDetailId, userId);
    }

    // Phương thức tạo mới một đánh giá
    public Reviews createReview(Reviews reviews) {
        // Kiểm tra nếu sản phẩm đã được đánh giá bởi người dùng hay chưa
        if (getReviewByProductDetailIdAndUserId(reviews.getProductDetails().getProductDetailId(), reviews.getUser().getUserId()) != null) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi.");
        }
        reviews.setReviewDate(new Date()); // Đảm bảo có thời gian review
        return reviewsRepository.save(reviews);
    }

    // Phương thức cập nhật một đánh giá
    public Reviews updateReview(Reviews reviews) {
        // Kiểm tra nếu reviewsId là 0, tức là chưa được gán giá trị hợp lệ
        if (reviews.getReviewsId() == 0 || !reviewsRepository.existsById(reviews.getReviewsId())) {
            throw new RuntimeException("Đánh giá không tồn tại.");
        }
        return reviewsRepository.save(reviews);
    }



    // Phương thức xóa đánh giá
    public void deleteReview(Long reviewsId) {
        if (!reviewsRepository.existsById(reviewsId)) {
            throw new RuntimeException("Đánh giá không tồn tại.");
        }
        reviewsRepository.deleteById(reviewsId);
    }
}
