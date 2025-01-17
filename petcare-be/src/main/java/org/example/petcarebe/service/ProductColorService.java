package org.example.petcarebe.service;


import org.example.petcarebe.model.ProductColors;
import org.example.petcarebe.repository.ProductColorsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductColorService {
    @Autowired
    private ProductColorsRepository productColorsRepository;

    public List<ProductColors> getAllProductColors() {return productColorsRepository.findAll();}

    public List<ProductColors> getActiveProductColors() {
        return productColorsRepository.findAll().stream()
                .filter(ProductColors::getStatus)
                .toList();
    }

    public Optional<ProductColors> getProductColorById(long id) {return productColorsRepository.findById(id);}

    public ProductColors saveProductColor(ProductColors productColor) {
        return productColorsRepository.save(productColor);
    }

    public void deleteProductColor(long id) {
        if(productColorsRepository.existsById(id)) {
            productColorsRepository.deleteById(id);
        }else{
            throw new RuntimeException("Không tìm thấy màu sản phẩm cho id này ::"+ id);
        }
    }

    public ProductColors updateProductColor(Long id, ProductColors productColorDetails) {
        ProductColors productColor = productColorsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Màu sản phẩm cho id này :: " + id));

        productColor.setColorValue(productColorDetails.getColorValue());
        productColor.setStatus(productColorDetails.getStatus());

        return productColorsRepository.save(productColor);
    }

}
