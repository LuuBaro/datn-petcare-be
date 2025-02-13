package org.example.petcarebe.service;

import lombok.RequiredArgsConstructor;
import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.CartDetailsRepository;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartDetailsService {
    private final CartDetailsRepository cartDetailsRepository;
    private final UserRepository userRepository;
    private final ProductDetailsRepository productDetailsRepository;

    @Autowired
    public CartDetailsService(CartDetailsRepository cartDetailsRepository,
                              UserRepository userRepository,
                              ProductDetailsRepository productDetailsRepository) {
        this.cartDetailsRepository = cartDetailsRepository;
        this.userRepository = userRepository;
        this.productDetailsRepository = productDetailsRepository;
    }

    // Lấy tất cả cart details của một user
    public List<CartDetails> getCartDetailsByUserId(Long userId) {
        return cartDetailsRepository.findByUser_UserId(userId);
    }

    // Lấy cart details theo product detail ID
    public List<CartDetails> getCartDetailsByProductDetailId(Long productDetailId) {
        return cartDetailsRepository.findByProductDetailsProductDetailId(productDetailId);
    }

    // Lấy cart details theo ID
    public CartDetails getCartDetailsById(Long cartDetailId) {
        return cartDetailsRepository.findById(cartDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cart detail với id: " + cartDetailId));
    }

    // Thêm một item vào giỏ hàng
    public CartDetails addToCart(Long userId, Long productDetailId, int quantity) {
        // Kiểm tra xem người dùng có tồn tại không
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("User found: " + user.getFullName());

        // Kiểm tra xem sản phẩm có tồn tại không
        ProductDetails productDetails = productDetailsRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        System.out.println("Product found: " + productDetails.getProducts().getProductName());

        // Tạo CartDetails mới và lưu vào DB
        CartDetails cartDetails = new CartDetails();
        cartDetails.setUser(user);
        cartDetails.setProductDetails(productDetails);
        cartDetails.setQuantityItem(quantity);

        // In ra thông tin cartDetails
        System.out.println("Adding to cart: " + cartDetails);

        return cartDetailsRepository.save(cartDetails);
    }



    // Cập nhật số lượng sản phẩm trong giỏ hàng
    public CartDetails updateCartDetails(Long cartDetailId, int newQuantity) {
        Optional<CartDetails> cartDetailsOptional = cartDetailsRepository.findById(cartDetailId);
        if (cartDetailsOptional.isPresent()) {
            CartDetails cartDetails = cartDetailsOptional.get();
            cartDetails.setQuantityItem(newQuantity);
            return cartDetailsRepository.save(cartDetails);
        } else {
            throw new RuntimeException("Không tìm thấy cart detail với id: " + cartDetailId);
        }
    }

    // Xóa item khỏi giỏ hàng
    public void removeFromCart(Long cartDetailId) {
        if (cartDetailsRepository.existsById(cartDetailId)) {
            cartDetailsRepository.deleteById(cartDetailId);
        } else {
            throw new RuntimeException("Không tìm thấy cart detail với id: " + cartDetailId);
        }
    }
}
