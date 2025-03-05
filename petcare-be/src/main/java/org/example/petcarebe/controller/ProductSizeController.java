package org.example.petcarebe.controller;


import jakarta.validation.Valid;
import org.example.petcarebe.model.ProductSizes;
import org.example.petcarebe.service.ProductSizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product-sizes")
public class ProductSizeController {

    @Autowired
    private ProductSizeService productSizeService;

    @GetMapping
    public List<ProductSizes> getAllProductSizes() {return productSizeService.getAllSize();}

    @GetMapping("/activeProductSizes")
    public List<ProductSizes> getActiveProductSizes() {
        return productSizeService.getActiveSize();
    }

    @GetMapping("/getProductSize/{productSizeId}")
    public ResponseEntity<ProductSizes> getProductWeightById(@PathVariable Long productSizeId) {
        Optional<ProductSizes> productSize = productSizeService.getProductSizesById(productSizeId);
        return productSize.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/createProductSize")
    public ResponseEntity<ProductSizes> createProductSize(@RequestBody @Valid ProductSizes productSizes) {
        ProductSizes createProductSize = productSizeService.createSize(productSizes);
        return ResponseEntity.status(201).body(createProductSize);
    }

    @PutMapping("/updateProductSize/{productSizeId}")
    public ResponseEntity<ProductSizes> updateProductSize(
            @PathVariable Long productSizeId,
            @Valid @RequestBody ProductSizes productSizes) {
        try {
            // Gọi service để thực hiện cập nhật
            ProductSizes updatedProductSize = productSizeService.updateProductSizes(productSizeId, productSizes);

            // Trả về đối tượng đã được cập nhật
            return ResponseEntity.ok(updatedProductSize);
        } catch (RuntimeException e) {
            // Xử lý nếu không tìm thấy đối tượng
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }


    @DeleteMapping("deleteProductSize/{productSizeId}")
    public ResponseEntity<ProductSizes> deleteProductSize(@PathVariable Long productSizeId) {
        boolean isDeleted = productSizeService.deleteProductSize(productSizeId);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
