package org.example.petcarebe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteDTO {
    private Long productId;
    private String productName;
    private String image;
    private Long userId;
    private Long favoritesId;
    private Date like_date;
    private boolean isLiked;
    private float price;

}
