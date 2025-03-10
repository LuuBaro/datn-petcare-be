package org.example.petcarebe.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.petcarebe.dto.CartDetailsDTO;
import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.model.ProductDetails;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.CartDetailsRepository;
import org.example.petcarebe.repository.ProductDetailsRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartDetailsService {

    @Autowired
    private CartDetailsRepository cartDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private ProductDetailsRepository productDetailsRepository;

    public List<CartDetails> getAllCartDetails() {
        return cartDetailsRepository.findAll();
    }

    public CartDetails getCartDetailsById(long id) {
        return cartDetailsRepository.findById(id).orElse(null);
    }


    public CartDetails addCartDetails(Long userId, Long productDetailId, int quantityItem) {
        // Kiểm tra xem User và ProductDetail có tồn tại không
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );
        ProductDetails productDetails = productDetailsRepository.findById(productDetailId).orElseThrow(() ->
                new IllegalArgumentException("Product details not found")
        );

        // Kiểm tra số lượng tồn kho
        int availableQuantity = productDetails.getQuantity();
        CartDetails existingCartDetail = cartDetailsRepository.findByUserAndProductDetails(user, productDetails);
        int currentQuantityInCart = existingCartDetail != null ? existingCartDetail.getQuantityItem() : 0;

        if (currentQuantityInCart + quantityItem > availableQuantity) {
            throw new IllegalArgumentException(
                    "Không thể thêm vào giỏ hàng vì sản phẩm trong giỏ hàng của bạn vượt quá số lượng"
            );
        }

        CartDetails cartDetail;
        if (existingCartDetail != null) {
            existingCartDetail.setQuantityItem(currentQuantityInCart + quantityItem);
            cartDetail = cartDetailsRepository.save(existingCartDetail);
        } else {
            CartDetails newCartDetail = new CartDetails();
            newCartDetail.setUser(user);
            newCartDetail.setProductDetails(productDetails);
            newCartDetail.setQuantityItem(quantityItem);
            cartDetail = cartDetailsRepository.save(newCartDetail);
        }

        // Gửi thông báo WebSocket sau khi thêm sản phẩm
        sendCartUpdate(userId);
        return cartDetail;
    }


    public CartDetails updateCartDetails(Long id, int quantityItem) {
        Optional<CartDetails> cartOpt = cartDetailsRepository.findById(id);
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("Cart item not found");
        }

        CartDetails cartDetails = cartOpt.get();
        ProductDetails productDetails = cartDetails.getProductDetails();

        if (quantityItem > productDetails.getQuantity()) {
            throw new IllegalArgumentException("Số lượng đặt hàng vượt quá số lượng tồn kho");
        }

        cartDetails.setQuantityItem(quantityItem);
        CartDetails updatedCart = cartDetailsRepository.save(cartDetails);

        // Gửi thông báo WebSocket sau khi cập nhật
        sendCartUpdate(cartDetails.getUser().getUserId());
        return updatedCart;
    }


    @Transactional
    public void deleteCartDetails(long id) {
        Optional<CartDetails> cartOpt = cartDetailsRepository.findById(id);
        if (cartOpt.isPresent()) {
            Long userId = cartOpt.get().getUser().getUserId();
            cartDetailsRepository.deleteById(id);
            // Gửi thông báo WebSocket sau khi xóa
            sendCartUpdate(userId);
        }
    }


    public List<CartDetailsDTO> getCartDetailsByUserId(Long userId) {
        List<Object[]> rawData = cartDetailsRepository.findRawCartDetailsByUserId(userId);

        return rawData.stream().map(obj -> new CartDetailsDTO(
                ((Number) obj[0]).longValue(),  // cartDetailId
                ((Number) obj[1]).longValue(),  // productDetailId
                (String) obj[2],                // image
                (String) obj[3],                // productName
                ((Number) obj[4]).floatValue(), // price
                (String) obj[5],                // colorValue
                (String) obj[6],                // sizeValue
                ((Number) obj[7]).floatValue(), // weightValue
                ((Number) obj[8]).intValue(),   // quantityItem
                (String) obj[9]                 // description
        )).collect(Collectors.toList());
    }


    @Modifying
    @Transactional
    public void clearCartDetailsByUserId(Long userId) {
        cartDetailsRepository.deleteByUserId(userId);
        // Gửi thông báo WebSocket sau khi xóa toàn bộ giỏ hàng
        sendCartUpdate(userId);
    }
    @PersistenceContext
    private EntityManager entityManager;


    @Transactional
    public void removeProductFromCart(Long productDetailId) {
        List<CartDetails> cartItems = cartDetailsRepository.findByProductDetails_ProductDetailId(productDetailId);
        if (!cartItems.isEmpty()) {
            Long userId = cartItems.get(0).getUser().getUserId(); // Lấy userId từ mục đầu tiên
            cartDetailsRepository.deleteCartItemByProductDetailId(productDetailId);
            entityManager.flush();
            sendCartUpdate(userId); // Gửi thông báo WebSocket với userId chính xác
        }
    }


    // Hàm gửi thông báo qua WebSocket sử dụng WebSocketService
    private void sendCartUpdate(Long userId) {
        List<CartDetails> cartItems = cartDetailsRepository.findByUserUserId(userId);
        int cartCount = cartItems.size(); // Đếm số lượng sản phẩm trong giỏ hàng
        webSocketService.sendToTopic("/topic/cart/" + userId, String.valueOf(cartCount)); // Gửi cartCount đến topic
    }






}
