package org.example.petcarebe.controller;

import jakarta.validation.Valid;
import org.example.petcarebe.dto.ProductListDTO;
import org.example.petcarebe.dto.ProductSummaryDTO;
import org.example.petcarebe.dto.ProductsDTO;
import org.example.petcarebe.model.Brand;
import org.example.petcarebe.model.Categories;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.repository.ProductRepository;
import org.example.petcarebe.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    @Autowired
    private ProductsService productsService;

    // Lấy một Product theo ID
    @GetMapping("/getByIdProducts/{productsId}")
    public ResponseEntity<Products> getProductsById(@PathVariable Long productsId) {
        Products product = productsService.getProductsById(productsId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    // Tạo mới một Product
    @PostMapping("/createProducts")
    public ResponseEntity<Products> createProducts(@Valid @RequestBody Products products) {
        try {
            Products createdProduct = productsService.createProduct(products);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Cập nhật Product theo ID
    @PutMapping("/updateProducts/{productId}")
    public ResponseEntity<Products> updateProduct(
            @PathVariable Long productId,
            @RequestBody @Valid Products updatedProduct) {
        try {
            Products updated = productsService.update(productId, updatedProduct);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // Xóa một Product theo ID
    @DeleteMapping("/deleteProducts/{productsId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productsId) {
        try {
            productsService.delete(productsId);
            return ResponseEntity.ok("Sản phẩm với ID " + productsId + " đã được xóa thành công.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sản phẩm không tồn tại.");
        }
    }

    @GetMapping("/getAllProductss")
    public ResponseEntity<List<ProductsDTO>> getAllProductss() {
        List<ProductsDTO> productsDTOList = productsService.getAllProductss();
        return ResponseEntity.ok(productsDTOList);
    }

    @GetMapping("/getAllProductsList")
    public ResponseEntity<List<ProductListDTO>> getAllProductsList() {
        List<ProductListDTO> productsDTOList = productsService.getAllProductsList();
        return ResponseEntity.ok(productsDTOList);
    }

    // API lấy thông tin sản phẩm theo ID
    @GetMapping("/products-summary/{productId}")
    public ResponseEntity<List<ProductSummaryDTO>> getProductByProductId(@PathVariable Long productId) {
        try {
            List<ProductSummaryDTO> products = productsService.getProductSummaryByProductId(productId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // API lấy danh sách tất cả sản phẩm
    @GetMapping("/products-summary")
    public ResponseEntity<List<ProductSummaryDTO>> getAllProducts() {
        List<ProductSummaryDTO> products = productsService.getAllProductSummaries();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Products>> searchProducts(@RequestParam(required = false) String keyword) {
        List<Products> products = productsService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }
}