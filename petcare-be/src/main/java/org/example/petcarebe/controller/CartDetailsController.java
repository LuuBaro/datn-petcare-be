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
    public ResponseEntity<?> addCartDetails(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.parseLong(payload.get("userId").toString());
            Long productDetailId = Long.parseLong(payload.get("productDetailId").toString());
            int quantityItem = (int) payload.get("quantityItem");

            // Gọi service để thêm chi tiết giỏ hàng
            CartDetails savedCartDetails = cartDetailsService.addCartDetails(userId, productDetailId, quantityItem);
            return ResponseEntity.ok(savedCartDetails);

        } catch (IllegalArgumentException e) {
            // Nếu có lỗi logic từ Service (ví dụ: vượt quá tồn kho), trả về lỗi 400 cùng thông báo
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Nếu có lỗi khác, trả về lỗi 500 cùng thông báo
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred."));
        }
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
