package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
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

    // Lấy tất cả đánh giá của một đơn hàng (orderDetailsId)
    @GetMapping("/order/{orderDetailsId}")
    public ResponseEntity<List<Reviews>> getReviewsByOrderDetails(@PathVariable Long orderDetailsId) {
        try {
            List<Reviews> reviews = reviewsService.getReviewsByOrderDetails(orderDetailsId);
            if (reviews.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Lấy tất cả đánh giá của một người dùng (userId)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reviews>> getReviewsByUser(@PathVariable Long userId) {
        try {
            List<Reviews> reviews = reviewsService.getReviewsByUser(userId);
            if (reviews.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


}
