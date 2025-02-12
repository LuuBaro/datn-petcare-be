package org.example.petcarebe.controller;


import org.checkerframework.checker.units.qual.A;
import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.ProductRepository;
import org.example.petcarebe.repository.UserRepository;
import org.example.petcarebe.service.FavoritesService;
import org.example.petcarebe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/favourites")
public class FavoritesController {
    @Autowired
    private FavoritesService favoritesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;
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


    @PostMapping
    public ResponseEntity<Favorites> addOrUpdateFavourite(@RequestBody Map<String, Object> request) {
        try {
            Long userId = ((Number) request.get("userId")).longValue();
            Long productId = ((Number) request.get("productId")).longValue();
            boolean isLiked = (boolean) request.get("isLiked");
            Date likeDate = new Date(); // Hoặc lấy từ request nếu cần

            // Lấy User và Product từ database
            User user = userRepository.findById(userId).orElse(null);
            Products product = productRepository.findById(productId).orElse(null);

            if (user == null || product == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // Tạo hoặc cập nhật Favorites
            Favorites favorites = new Favorites();
            favorites.setUser(user);
            favorites.setProducts(product);
            favorites.setLiked(isLiked);
            favorites.setLike_date(likeDate);

            Favorites savedFavourite = favoritesService.addOrUpdateFavourite(favorites);
            return ResponseEntity.ok(savedFavourite);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
