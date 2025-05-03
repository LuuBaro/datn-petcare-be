package org.example.petcarebe.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.petcarebe.dto.CartDetailsDTO;
import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.service.CartDetailsService;
import org.example.petcarebe.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart-details")
public class CartDetailsController {

    @Autowired
    private CartDetailsService cartDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private HttpServletRequest request;

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
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> addCartDetails(@RequestBody Map<String, Object> payload) {
        try {
            // Lấy thông tin người dùng từ SecurityContextHolder
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Lấy token từ header Authorization
            String token = getTokenFromRequest();
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token không được cung cấp."));
            }

            // Trích xuất userId từ token
            Long userIdFromToken = jwtService.extractUserId(token);

            // Lấy userId từ body
            Long userIdFromBody = Long.parseLong(payload.get("userId").toString());

            // Kiểm tra xem userId trong body có khớp với userId trong token hay không
            if (!userIdFromToken.equals(userIdFromBody)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "userId trong request không khớp với người dùng đã xác thực."));
            }

            Long productDetailId = Long.parseLong(payload.get("productDetailId").toString());
            int quantityItem = (int) payload.get("quantityItem");

            // Gọi service để thêm chi tiết giỏ hàng
            CartDetails savedCartDetails = cartDetailsService.addCartDetails(userIdFromBody, productDetailId, quantityItem);
            return ResponseEntity.ok(savedCartDetails);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã xảy ra lỗi không mong muốn."));
        }
    }

    // Hàm lấy token từ header Authorization
    private String getTokenFromRequest() {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCartDetails(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        try {
            int quantityItem = payload.get("quantityItem");
            CartDetails updatedCart = cartDetailsService.updateCartDetails(id, quantityItem);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCartDetails(@PathVariable long id) {
        cartDetailsService.deleteCartDetails(id);
        return ResponseEntity.noContent().build();
    }


    // API lấy giỏ hàng theo userId
    @GetMapping("/findByCart/{userId}")
    public ResponseEntity<List<CartDetailsDTO>> getCartDetails(@PathVariable Long userId) {
        System.out.println("Received userId: " + userId);

        List<CartDetailsDTO> cartDetails = cartDetailsService.getCartDetailsByUserId(userId);


        return ResponseEntity.ok(cartDetails);
    }
}
