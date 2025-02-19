package org.example.petcarebe.controller;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.model.Reviews;
import org.example.petcarebe.service.ReviewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;

    // API để lấy tất cả đánh giá của sản phẩm theo productDetailId
    @GetMapping("/product/{productDetailId}")
    public ResponseEntity<List<Reviews>> getReviewsByProductDetailId(@PathVariable Long productDetailId) {
        List<Reviews> reviews = reviewsService.getReviewsByProductDetailId(productDetailId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    // API để lấy tất cả đánh giá của người dùng theo userId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reviews>> getReviewsByUserId(@PathVariable Long userId) {
        List<Reviews> reviews = reviewsService.getReviewsByUserId(userId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    // API để tạo mới một đánh giá
    @PostMapping
    public ResponseEntity<Reviews> createReview(@RequestBody Reviews reviews) {
        Reviews createdReview = reviewsService.createReview(reviews);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // API để cập nhật một đánh giá
    @PutMapping
    public ResponseEntity<Reviews> updateReview(@RequestBody Reviews reviews) {
        Reviews updatedReview = reviewsService.updateReview(reviews);
        return new ResponseEntity<>(updatedReview, HttpStatus.OK);
    }

    // API để xóa một đánh giá
    @DeleteMapping("/{reviewsId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewsId) {
        reviewsService.deleteReview(reviewsId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
