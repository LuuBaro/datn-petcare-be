package org.example.petcarebe.service;

import org.example.petcarebe.dto.FavoriteDTO;
import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.FavoritesRepository;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.repository.ProductRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoritesService {
    private final FavoritesRepository favoritesRepository;
    private final UserRepository userRepository;
    private final ProductRepository productsRepository;
    private final ProductDetailsRepository productDetailsRepository;
    public FavoritesService(FavoritesRepository favoritesRepository, UserRepository userRepository, ProductRepository productsRepository, ProductDetailsRepository productDetailsRepository) {
        this.favoritesRepository = favoritesRepository;
        this.userRepository = userRepository;
        this.productsRepository = productsRepository;
        this.productDetailsRepository = productDetailsRepository;
    }

    // Toggle yêu thích
    public FavoriteDTO toggleFavorite(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

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

        favoritesRepository.save(favorite);
        // Chuyển đổi thành FavoriteDTO để trả về

        // Lấy giá sản phẩm từ ProductDetails (ví dụ: lấy giá đầu tiên trong danh sách)
        float price = product.getProductDetails() != null && !product.getProductDetails().isEmpty()
                ? product.getProductDetails().get(0).getPrice()
                : 0.0f; // Giá mặc định nếu không có thông tin
        return new FavoriteDTO(
                product.getProductId(),
                product.getProductName(),
                product.getImage(),

                user.getUserId(),
                favorite.getFavoritesId(),
                favorite.getLike_date(),

                favorite.isLiked(),
                price
        );
    }

    // Kiểm tra trạng thái yêu thích
    public boolean getFavoriteStatus(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

        return favoritesRepository.existsByUserAndProductsAndIsLiked(user, product, true);
    }

    public List<FavoriteDTO> getFavoriteProducts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Favorites> favorites = favoritesRepository.findAllByUserAndIsLikedTrue(user);

        return favorites.stream()
                .map(fav -> {
                    Products product = fav.getProducts();

                    // Lấy giá từ ProductDetails (lấy giá đầu tiên nếu có)
                    float price = product.getProductDetails() != null && !product.getProductDetails().isEmpty()
                            ? product.getProductDetails().get(0).getPrice()
                            : 0.0f; // Giá mặc định nếu không có thông tin

                    return new FavoriteDTO(
                            product.getProductId(),
                            product.getProductName(),
                            product.getImage(),
                            user.getUserId(),
                            fav.getFavoritesId(),
                            fav.getLike_date(),
                            fav.isLiked(),
                            price
                    );
                })
                .collect(Collectors.toList());
    }
}
