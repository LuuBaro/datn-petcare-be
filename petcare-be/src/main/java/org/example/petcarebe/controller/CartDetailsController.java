package org.example.petcarebe.controller;

import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.service.CartDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartDetailsController {

    private final CartDetailsService cartDetailsService;

    @Autowired
    public CartDetailsController(CartDetailsService cartDetailsService) {
        this.cartDetailsService = cartDetailsService;
    }

    // Lấy tất cả cart details của user
    @GetMapping("/user/{userId}")
    public List<CartDetails> getCartDetailsByUserId(@PathVariable Long userId) {
        return cartDetailsService.getCartDetailsByUserId(userId);
    }

    // Lấy cart details theo product detail ID
    @GetMapping("/product/{productDetailId}")
    public List<CartDetails> getCartDetailsByProductDetailId(@PathVariable Long productDetailId) {
        return cartDetailsService.getCartDetailsByProductDetailId(productDetailId);
    }

    // Lấy cart detail theo ID
    @GetMapping("/{cartDetailId}")
    public CartDetails getCartDetailsById(@PathVariable Long cartDetailId) {
        return cartDetailsService.getCartDetailsById(cartDetailId);
    }

    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/add/{userId}/{productDetailId}/{quantity}")
    public ResponseEntity<CartDetails> addToCart(@PathVariable Long userId,
                                                 @PathVariable Long productDetailId,
                                                 @PathVariable int quantity) {
        System.out.println("Adding to cart: userId=" + userId + ", productDetailId=" + productDetailId);
        CartDetails cartDetails = cartDetailsService.addToCart(userId, productDetailId, quantity);
        System.out.println("Cart added: " + cartDetails);
        return ResponseEntity.ok(cartDetails);
    }





    // Cập nhật sản phẩm trong giỏ hàng
    @PutMapping("/update/{cartDetailId}")
    public ResponseEntity<CartDetails> updateCartDetails(@PathVariable Long cartDetailId,
                                                         @RequestParam int quantity) {
        CartDetails cartDetails = cartDetailsService.updateCartDetails(cartDetailId, quantity);
        return ResponseEntity.ok(cartDetails);
    }

    // Xóa sản phẩm khỏi giỏ hàng
    @DeleteMapping("/remove/{cartDetailId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long cartDetailId) {
        cartDetailsService.removeFromCart(cartDetailId);
        return ResponseEntity.noContent().build();
    }
}
