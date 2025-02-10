package org.example.petcarebe.controller;

import org.example.petcarebe.dto.CartDetailsDTO;
import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.service.CartDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart-details")
public class CartDetailsController {

    @Autowired
    private CartDetailsService cartDetailsService;

    @GetMapping("/all")
    public List<CartDetails> getAllCartDetails() {
        return cartDetailsService.getAllCartDetails();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDetails> getCartDetailsById(@PathVariable long id) {
        CartDetails cartDetails = cartDetailsService.getCartDetailsById(id);
        if (cartDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cartDetails);
    }

    @PostMapping("/add")
    public ResponseEntity<CartDetails> addCartDetails(@RequestBody Map<String, Object> payload) {
        Long userId = Long.parseLong(payload.get("userId").toString());
        Long productDetailId = Long.parseLong(payload.get("productDetailId").toString());
        int quantityItem = (int) payload.get("quantityItem");

        CartDetails savedCartDetails = cartDetailsService.addCartDetails(userId, productDetailId, quantityItem);
        return ResponseEntity.ok(savedCartDetails);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CartDetails> updateCartDetails(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        int quantityItem = payload.get("quantityItem");

        CartDetails updatedCart = cartDetailsService.updateCartDetails(id, quantityItem);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCartDetails(@PathVariable long id) {
        cartDetailsService.deleteCartDetails(id);
        return ResponseEntity.noContent().build();
    }


    // API lấy giỏ hàng theo userId
    @GetMapping("/findByCart/{userId}")
    public ResponseEntity<?> getCartDetails(@PathVariable Long userId) {
        System.out.println("Received userId: " + userId);

        List<CartDetailsDTO> cartDetails = cartDetailsService.getCartDetailsByUserId(userId);

        if (cartDetails.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No cart details found for userId: " + userId);
        }

        return ResponseEntity.ok(cartDetails);
    }

}
