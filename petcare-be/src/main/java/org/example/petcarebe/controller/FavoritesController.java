package org.example.petcarebe.controller;

import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.service.FavoritesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {
    private final FavoritesService favoritesService;

    public FavoritesController(FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    @PostMapping("/toggle")
    public ResponseEntity<Favorites> toggleFavorite(@RequestParam Long userId, @RequestParam Long productId) {
        Favorites favorite = favoritesService.toggleFavorite(userId, productId);
        return ResponseEntity.ok(favorite);
    }


}
