package org.example.petcarebe.repository;

import org.example.petcarebe.model.CartDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartDetailsRepository extends JpaRepository<CartDetails, Long> {
    List<CartDetails> findByUser_UserId(Long userId);
    List<CartDetails> findByProductDetailsProductDetailId(Long productDetailId);
    Optional<CartDetails> findByUser_UserIdAndProductDetailsProductDetailId(Long userId, Long productDetailId);

}

