package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductsController {
    @Autowired
    private ProductsService productsService;

    // Lấy danh sách tất cả Products


    // Lấy một Product theo ID
    @GetMapping("/getByIdProducts/{productsId}")
    public Products getProductsById(@PathVariable Long productsId) {
        return productsService.getProductsById(productsId);
    }

    // Tạo mới một Product
    @PostMapping("/createProducts")
    public ResponseEntity<Products> createProducts(@Valid @RequestBody Products products) {
        Products createdProduct = productsService.createProduct(products);
        return ResponseEntity.status(201).body(createdProduct);  // Trả về HTTP 201 (Created)
    }

    // Cập nhật Product theo ID
    @PutMapping("/updateProducts/{productId}")
    public ResponseEntity<Products> updateProduct(
            @PathVariable Long productId,
            @RequestBody @Valid Products updatedProduct) {

        try {
            Products updated = productsService.update(productId, updatedProduct);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // Xóa một Product theo ID
    @DeleteMapping("/deleteProducts/{productsId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productsId) {
        productsService.delete(productsId);
        return ResponseEntity.ok("Sản phẩm với ID " + productsId + " đã được xóa thành công.");
    }

    @GetMapping("/getAllProductss")
    public ResponseEntity<List<ProductsDTO>> getAllProductss() {
        List<ProductsDTO> productsDTOList = productsService.getAllProductss();
        return ResponseEntity.ok(productsDTOList);
    }

}
