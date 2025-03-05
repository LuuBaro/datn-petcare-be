package org.example.petcarebe.service;


import org.example.petcarebe.model.ProductSizes;
import org.example.petcarebe.repository.ProductSizesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductSizeService {
    @Autowired
    private ProductSizesRepository productSizesRepository;

    public List<ProductSizes> getAllSize() {
        return productSizesRepository.findAll();
    }

    public List<ProductSizes> getActiveSize() {
        return productSizesRepository.findAll().stream()
                .filter(ProductSizes::getStatus)
                .toList();
    }

    public ProductSizes createSize(ProductSizes productSizes) {
        return productSizesRepository.save(productSizes);
    }

    public Optional<ProductSizes> getProductSizesById(Long id) {
        return productSizesRepository.findById(id);
    }

    public ProductSizes updateProductSizes(Long id, ProductSizes sizesDetails) {
        // Tìm kiếm ProductSizes theo ID
        ProductSizes sizes = productSizesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Kích cỡ sản phẩm cho id này :: " + id));

        // Cập nhật giá trị sizeValue và status
        sizes.setSizeValue(sizesDetails.getSizeValue());
        sizes.setStatus(sizesDetails.getStatus());

        // Lưu đối tượng đã cập nhật vào cơ sở dữ liệu
        return productSizesRepository.save(sizes);
    }

    public boolean deleteProductSize(Long id) {
        if (productSizesRepository.existsById(id)) {
            productSizesRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
