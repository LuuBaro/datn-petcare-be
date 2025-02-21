package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.ReviewDTO;
import org.example.petcarebe.model.Reviews;
import org.example.petcarebe.service.ReviewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    @Autowired
    private ReviewsService reviewsService;

    // Đánh giá sản phẩm (thêm đánh giá)
    @PostMapping("/add")
    public ResponseEntity<Reviews> addReview(@RequestBody Reviews review) {
        try {
            Reviews savedReview = reviewsService.addReview(review);
            return ResponseEntity.ok(savedReview);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // API lấy đánh giá theo OrderDetailId
//    @GetMapping("/order-detail/{orderDetailId}")
//    public ResponseEntity<List<ReviewDTO>> getReviewsByOrderDetail(@PathVariable Long orderDetailId) {
//        List<ReviewDTO> reviews = reviewsService.getReviewsByOrderDetails(orderDetailId);
//        return ResponseEntity.ok(reviews);
//    }

    // API lấy đánh giá theo UserId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewDTO> reviews = reviewsService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    // API lấy đánh giá theo ProductDetailId
//    @GetMapping("/product-detail/{productDetailId}")
//    public ResponseEntity<List<ReviewDTO>> getReviewsByProductDetail(@PathVariable Long productDetailId) {
//        List<ReviewDTO> reviews = reviewsService.getReviewsByOrderDetails(productDetailId);
//        return ResponseEntity.ok(reviews);
//    }


    // API lấy đánh giá theo ProductDetailId
    @GetMapping("/product-detail/{productDetailId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProductDetail(@PathVariable Long productDetailId) {
        List<ReviewDTO> reviews = reviewsService.getReviewsByProductDetail(productDetailId);
        return ResponseEntity.ok(reviews);
    }

}
