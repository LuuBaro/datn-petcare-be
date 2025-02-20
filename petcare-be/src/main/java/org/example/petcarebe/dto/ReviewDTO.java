package org.example.petcarebe.dto;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    private Long reviewId;
    private float rating;
    private String comment;
    private Date reviewDate;

    private Long userId;
    private String userName; // Thêm để lấy tên người đánh giá
    private String imageUrl;

    private Long productDetailId;
    private String productName;
    private String productImageUrl; // Ảnh sản phẩm
}
