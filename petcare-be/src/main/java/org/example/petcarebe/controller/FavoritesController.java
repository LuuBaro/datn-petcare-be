package org.example.petcarebe.controller;


import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.example.petcarebe.service.FavoritesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/api/favourites")
public class FavoritesController {
    private FavoritesService favoritesService;

    @Autowired
    public FavoritesController(FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    // Get all favourites by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Favorites>> getFavouritesByUser(@PathVariable Long userId) {
        User user = new User();
        user.setUserId(userId);
        List<Favorites> favourites = favoritesService.getFavoritesByUser(user);
        return ResponseEntity.ok(favourites);
    }

    // Get all favourites for a product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Favorites>> getFavouritesByProduct(@PathVariable Long productId) {
        Products product = new Products();
        product.setProductId(productId);
        List<Favorites> favourites = favoritesService.getFavouritesByProduct(product);
        return ResponseEntity.ok(favourites);
    }

    // Get a specific favourite by user and product
    @GetMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<Favorites> getFavouriteByUserAndProduct(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        User user = new User();
        user.setUserId(userId);

        Products product = new Products();
        product.setProductId(productId);

        Optional<Favorites> favourite = favoritesService.getFavouriteByUserAndProduct(user, product);
        return favourite.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // Add a favourite or update it
    @PostMapping
    public ResponseEntity<Favorites> addOrUpdateFavourite(@RequestBody Favorites favorites) {
        Favorites savedFavourite = favoritesService.addOrUpdateFavourite(favorites);
        return ResponseEntity.ok(savedFavourite);
    }

    // Remove a favourite by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavourite(@PathVariable Long id) {
        favoritesService.removeFavourite(id);
        return ResponseEntity.noContent().build();
    }

    // Remove a specific favourite by user and product
    @DeleteMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<Void> removeFavouriteByUserAndProduct(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        User user = new User();
        user.setUserId(userId);

        Products product = new Products();
        product.setProductId(productId);

        favoritesService.removeFavouriteByUserAndProduct(user, product);
        return ResponseEntity.noContent().build();
    }
}
