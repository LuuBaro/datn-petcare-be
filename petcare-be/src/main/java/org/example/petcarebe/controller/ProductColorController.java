package org.example.petcarebe.controller;


import jakarta.validation.Valid;
import org.example.petcarebe.model.ProductColors;
import org.example.petcarebe.service.ProductColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product-colors")
public class ProductColorController {

    @Autowired
    private ProductColorService productColorService;

    @GetMapping
    public List <ProductColors> getAllProductColors() {return productColorService.getAllProductColors();}

    @GetMapping("/activeProductColor")
    public List<ProductColors> getActiveColors() {
        return productColorService.getActiveProductColors();
    }

    @GetMapping("/getByProductColorId/{productColorId}")
    public Optional<ProductColors> getProductColors(@PathVariable long productColorId) {return productColorService.getProductColorById(productColorId);}

    @PostMapping("/createProductColor")
    public ResponseEntity<ProductColors> createProductColor(@RequestBody @Valid ProductColors productColors) {
        ProductColors createProductColor = productColorService.saveProductColor(productColors);
        return ResponseEntity.status(201).body(createProductColor);

    }

    @PutMapping("/updateProductColor/{productColorId}")
    public ResponseEntity<ProductColors> updateProductColor(@PathVariable Long productColorId, @Valid @RequestBody ProductColors productColorDetails) {
        try {
            ProductColors updatedProductColor = productColorService.updateProductColor(productColorId, productColorDetails);
            return new ResponseEntity<>(updatedProductColor, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/deleteProductColor/{productColorId}")
    public ResponseEntity<ProductColors> deleteProductColor(@PathVariable Long productColorId) {
        try {
            productColorService.deleteProductColor(productColorId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
