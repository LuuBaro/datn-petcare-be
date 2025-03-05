package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.ReviewDTO;
import org.example.petcarebe.model.Reviews;
import org.example.petcarebe.repository.ReviewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    // Lưu đánh giá
    public Reviews addReview(Reviews review) {
        return reviewsRepository.save(review);
    }

    // Lấy tất cả đánh giá của một sản phẩm (orderDetails), trả về DTO
//    public List<ReviewDTO> getReviewsByOrderDetails(Long orderDetailsId) {
//        return reviewsRepository.findByOrderDetails_OrderDetailsId(orderDetailsId)
//                .stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }

    // Lấy tất cả đánh giá của một người dùng, trả về DTO
    public List<ReviewDTO> getReviewsByUser(Long userId) {
        return reviewsRepository.findByUser_UserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Chuyển đổi từ Reviews Entity sang ReviewDTO
    private ReviewDTO convertToDTO(Reviews review) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewsId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewDate(review.getReviewDate())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getFullName()) // Giả sử User có thuộc tính fullName
                .imageUrl(review.getUser().getImageUrl())
                .productDetailId(review.getOrderDetails().getProductDetails().getProductDetailId()) // Lấy ID sản phẩm
                .productName(review.getOrderDetails().getProductDetails().getProducts().getProductName()) // Tên sản phẩm
                .productImageUrl(review.getOrderDetails().getProductDetails().getProducts().getImage()) // Ảnh sản phẩm
                .build();
    }

    // Lấy đánh giá theo productDetailId (tìm orderDetails trước)
    public List<ReviewDTO> getReviewsByProductDetail(Long productDetailId) {
        return reviewsRepository.findByProductDetailId(productDetailId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}
