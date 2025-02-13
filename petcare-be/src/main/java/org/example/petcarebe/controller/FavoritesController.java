package org.example.petcarebe.controller;

import org.example.petcarebe.dto.FavoriteDTO;
import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.service.FavoritesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {
    private final FavoritesService favoritesService;

    public FavoritesController(FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    // API: Toggle yêu thích (Thêm vào yêu thích hoặc bỏ yêu thích)
    @PostMapping("/toggle")
    public ResponseEntity<FavoriteDTO> toggleFavorite(@RequestParam Long userId, @RequestParam Long productId) {
        FavoriteDTO favoriteDTO = favoritesService.toggleFavorite(userId, productId);
        return ResponseEntity.ok(favoriteDTO);
    }

    // API: Kiểm tra trạng thái yêu thích của sản phẩm đối với người dùng
    @GetMapping("/status")
    public ResponseEntity<Boolean> getFavoriteStatus(@RequestParam Long userId, @RequestParam Long productId) {
        boolean status = favoritesService.getFavoriteStatus(userId, productId);
        return ResponseEntity.ok(status);
    }

    // API: Lấy danh sách sản phẩm yêu thích của người dùng
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<FavoriteDTO>> getFavoriteProducts(@PathVariable Long userId) {
        List<FavoriteDTO> favoriteProducts = favoritesService.getFavoriteProducts(userId);
        return ResponseEntity.ok(favoriteProducts);
    }
}
