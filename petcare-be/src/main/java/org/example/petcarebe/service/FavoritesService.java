package org.example.petcarebe.service;

import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.FavoritesRepository;
import org.example.petcarebe.repository.ProductRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class FavoritesService {
    private final FavoritesRepository favoritesRepository;
    private final UserRepository userRepository;
    private final ProductRepository productsRepository;

    public FavoritesService(FavoritesRepository favoritesRepository, UserRepository userRepository, ProductRepository productsRepository) {
        this.favoritesRepository = favoritesRepository;
        this.userRepository = userRepository;
        this.productsRepository = productsRepository;
    }

    public Favorites toggleFavorite(Long userId, Long productId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Products> productOpt = productsRepository.findById(productId);

        if (userOpt.isEmpty() || productOpt.isEmpty()) {
            throw new RuntimeException("User hoặc Product không tồn tại");
        }

        User user = userOpt.get();
        Products product = productOpt.get();
        Favorites favorite = favoritesRepository.findByUserAndProducts(user, product);

        if (favorite == null) {
            // Nếu chưa có trong danh sách yêu thích, tạo mới
            favorite = new Favorites();
            favorite.setUser(user);
            favorite.setProducts(product);
            favorite.setLike_date(new Date());
            favorite.setLiked(true);
        } else {
            // Nếu đã tồn tại, đảo trạng thái thích
            favorite.setLiked(!favorite.isLiked());
        }

        return favoritesRepository.save(favorite);
    }


}
