package org.example.petcarebe.repository;



import org.example.petcarebe.model.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {
    List<ProductImages> findByProductDetails_ProductDetailId(Long productDetailId);

}
