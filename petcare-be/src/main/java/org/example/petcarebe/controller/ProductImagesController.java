package org.example.petcarebe.controller;


import lombok.RequiredArgsConstructor;
import org.example.petcarebe.dto.ProductImagesDTO;
import org.example.petcarebe.model.ProductImages;
import org.example.petcarebe.service.ProductImagesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productImages")
@RequiredArgsConstructor
public class ProductImagesController {

    private final ProductImagesService productImagesService;

    // Lấy tất cả ảnh sản phẩm
    @GetMapping("/getAll")
    public List<ProductImages> getAllProductImages() {
        return productImagesService.getAllProductImages();
    }

    // Lấy tất cả ảnh sản phẩm
    @GetMapping("/getAllImage")
    public List<ProductImagesDTO> getAllProductImagesDTO() {
        return productImagesService.getAllProductImagesDTO();
    }

    @PostMapping("/addImage")
    public ResponseEntity<?> addProductImage(@RequestBody ProductImagesDTO productImagesDTO) {
        try {
            ProductImages savedImage = productImagesService.addProductImage(productImagesDTO);
            return ResponseEntity.ok(savedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi thêm ảnh: " + e.getMessage());
        }
    }


    // Lấy ảnh sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductImages> getProductImagesById(@PathVariable("id") Long productImagesId) {
        ProductImages productImages = productImagesService.getProductImagesById(productImagesId);
        if (productImages != null) {
            return ResponseEntity.ok(productImages);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Thêm mới ảnh sản phẩm
    @PostMapping("/add")
    public ResponseEntity<ProductImages> saveProductImages(@RequestBody ProductImages productImages) {
        ProductImages savedProductImages = productImagesService.saveProductImages(productImages);
        return ResponseEntity.ok(savedProductImages);
    }

    // Sửa ảnh sản phẩm theo ID
    @PutMapping("/update/{id}")
    public ResponseEntity<ProductImages> updateProductImages(
            @PathVariable("id") Long productImagesId,
            @RequestBody ProductImages productImages) {
        ProductImages updatedProductImages = productImagesService.updateProductImages(productImagesId, productImages);
        if (updatedProductImages != null) {
            return ResponseEntity.ok(updatedProductImages);
        } else {
            return ResponseEntity.notFound().build();
        }
    }



    // Xóa ảnh sản phẩm theo ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProductImages(@PathVariable("id") Long productImagesId) {
        productImagesService.deleteProductImages(productImagesId);
        return ResponseEntity.noContent().build();
    }


    // Lấy ảnh đầu tiên của sản phẩm theo ProductDetailId
    @GetMapping("/getFirstImage/{productDetailId}")
    public ResponseEntity<ProductImages> getFirstImageByProductDetailId(@PathVariable("productDetailId") Long productDetailId) {
        ProductImages firstImage = productImagesService.getFirstImageByProductDetails(productDetailId);
        if (firstImage != null) {
            return ResponseEntity.ok(firstImage);
        } else {
            return ResponseEntity.notFound().build(); // Nếu không có ảnh, trả về 404
        }
    }



    // Lấy tất cả ảnh theo ProductDetailId
    @GetMapping("/getImages/{productDetailId}")
    public ResponseEntity<List<ProductImagesDTO>> getAllImagesByProductDetailId(@PathVariable("productDetailId") Long productDetailId) {
        List<ProductImagesDTO> images = productImagesService.getAllImagesByProductDetails(productDetailId);
        if (!images.isEmpty()) {
            return ResponseEntity.ok(images);
        } else {
            return ResponseEntity.notFound().build(); // Nếu không có ảnh, trả về 404
        }
    }


    //huy update
    @PutMapping("/updates/{id}")
    public ResponseEntity<?> updateProductsImagesnew(
            @PathVariable("id") Long productImagesId,
            @RequestBody ProductImagesDTO productImagesDTO) {
        try {
            ProductImages updatedProductImages = productImagesService.updateProductsImagesnew(productImagesId, productImagesDTO);
            return ResponseEntity.ok(updatedProductImages);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        }

    }
}
