package org.example.petcarebe.service;

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

    @Autowired
    private CartDetailsRepository cartDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    public List<CartDetails> getAllCartDetails() {
        return cartDetailsRepository.findAll();
    }

    public CartDetails getCartDetailsById(long id) {
        return cartDetailsRepository.findById(id).orElse(null);
    }

    public CartDetails addCartDetails(Long userId, Long productDetailId, int quantityItem) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<ProductDetails> productOpt = productDetailsRepository.findById(productDetailId);

        if (userOpt.isEmpty() || productOpt.isEmpty()) {
            throw new IllegalArgumentException("User or Product not found");
        }

        CartDetails cartDetails = new CartDetails();
        cartDetails.setUser(userOpt.get());
        cartDetails.setProductDetails(productOpt.get());
        cartDetails.setQuantityItem(quantityItem);

        return cartDetailsRepository.save(cartDetails);
    }

    public CartDetails updateCartDetails(Long id, int quantityItem) {
        Optional<CartDetails> cartOpt = cartDetailsRepository.findById(id);
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("Cart item not found");
        }

        CartDetails cartDetails = cartOpt.get();
        cartDetails.setQuantityItem(quantityItem);
        return cartDetailsRepository.save(cartDetails);
    }

    public void deleteCartDetails(long id) {
        cartDetailsRepository.deleteById(id);
    }
}
